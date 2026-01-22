package com.flipkart.krystal.lattice.ext.mcp.quarkus.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_POJO_SUFFIX;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.ext.mcp.McpServerDopant;
import com.flipkart.krystal.lattice.ext.mcp.api.McpServer;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite.FacetDetail;
import com.flipkart.krystal.vajram.json.Json;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.Resource;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.Map.Entry;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import lombok.extern.slf4j.Slf4j;

/**
 * This provides a {@link CodeGenerator} which generates the following classes:
 *
 * <ul>
 *   <li>$LatticeAppName$_QuarkusMcpTools ($LatticeAppName$ is the name of the lattice application
 *       class)- this contains adapter methods - one for each vajram which has been configured as a
 *       toolVajram in the {@link McpServer} annotation placed on the Lattice Application class.
 *   <li>$LatticeAppName$_QuarkusMcpResources - this contains adapter methods - one for each vajram
 *       which has been configured as a resourceVajram in the {@link McpServer} annotation placed on
 *       the Lattice Application class.
 *   <li>$LatticeAppName$_QuarkusMcpResourceTemplates - this contains adapter methods - one for each
 *       annotation placed on the Lattice Application class. vajram which has been configured as a
 *       resourceTemplateVajram in the {@link McpServer}
 * </ul>
 */
@Slf4j
@AutoService(LatticeCodeGeneratorProvider.class)
public class QuarkusMcpCodegenProvider implements LatticeCodeGeneratorProvider {
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return () -> {
      TypeElement latticeAppTypeElement = latticeCodegenContext.latticeAppTypeElement();
      McpServer mcpServer = latticeAppTypeElement.getAnnotation(McpServer.class);
      if (mcpServer == null) {
        log.warn(
            "McpServer annotation is missing on lattice app {}. Skipping {}",
            latticeAppTypeElement,
            QuarkusMcpCodegenProvider.class);
        return;
      }
      if (latticeCodegenContext.codegenPhase() != CodegenPhase.FINAL) {
        return;
      }
      CodeGenUtility util = latticeCodegenContext.codeGenUtility().codegenUtil();
      generateMcpTools(latticeCodegenContext, util, latticeAppTypeElement, mcpServer);
      generateMcpResources(latticeCodegenContext, util, latticeAppTypeElement, mcpServer);
      generateMcpResourceTemplates(latticeCodegenContext, util, latticeAppTypeElement, mcpServer);
    };
  }

  private void generateMcpResourceTemplates(
      LatticeCodegenContext latticeCodegenContext,
      CodeGenUtility util,
      TypeElement latticeAppTypeElement,
      McpServer mcpServer) {
    List<? extends TypeMirror> resourceTemplateVajrams =
        util.getTypesFromAnnotationMember(mcpServer::resourceTemplateVajrams).stream().toList();
    ClassName resourceTemplatesClassName =
        ClassName.get(
            util.getPackageName(latticeAppTypeElement),
            latticeAppTypeElement.getSimpleName().toString() + "_QuarkusMcpResourceTemplates");

    TypeSpec.Builder resourceTemplatesClassBuilder =
        util.classBuilder(
                resourceTemplatesClassName.simpleName(),
                latticeAppTypeElement.getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addAnnotation(Singleton.class);
    addCommonMembers(resourceTemplatesClassBuilder);

    for (TypeMirror resourceTemplateVajram : resourceTemplateVajrams) {
      VajramInfo vajramInfo =
          latticeCodegenContext.codeGenUtility().computeVajramInfo(resourceTemplateVajram);
      MethodSpec.Builder methodBuilder = createUniMethodBuilder(vajramInfo, util);

      StringBuilder uriTemplate = new StringBuilder("vajram://").append(vajramInfo.vajramName());
      CodeBlock.Builder requestBuilder =
          CodeBlock.builder()
              .add(
                  "$T._builder()",
                  ClassName.get(
                      vajramInfo.lite().packageName(),
                      vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX));

      for (Entry<String, FacetDetail> entry : vajramInfo.lite().facetDetails().entrySet()) {
        String facetName = entry.getKey();
        FacetDetail facetDetail = entry.getValue();
        if (facetDetail.facetType() != FacetType.INPUT) {
          continue;
        }

        TypeName typeName =
            TypeName.get(facetDetail.dataType().javaModelType(util.processingEnv()));

        if (typeName.equals(ClassName.get(String.class))) {
          methodBuilder.addParameter(ParameterSpec.builder(typeName, facetName).build());
          uriTemplate.append("/{").append(facetName).append("}");
          requestBuilder.add(".$L($L)", facetName, facetName);
        } else if (typeName.equals(ClassName.get(URI.class))) {
          methodBuilder.addParameter(
              ParameterSpec.builder(ClassName.get(RequestUri.class), facetName).build());
          requestBuilder.add(
              ".$L($T.fromUri($L.value()).build())", facetName, UriBuilder.class, facetName);
        } else {
          util.error(
              "Resource Template vajrams can only accept String or URI parameters. Found: "
                  + typeName
                  + " for facet "
                  + facetName
                  + " in vajram "
                  + vajramInfo.vajramName());
        }
      }

      methodBuilder.addAnnotation(
          AnnotationSpec.builder(ResourceTemplate.class)
              .addMember("uriTemplate", "$S", uriTemplate.toString())
              .addMember("description", "$S", getDocString(vajramInfo))
              .build());

      methodBuilder.addCode(
          """
          return $T.createFrom()
              .completionStage(
                  mcpServerDopant.executeMcpResourceTemplate(
                    $L
                    ._build()));
          """,
          Uni.class,
          requestBuilder.build());

      resourceTemplatesClassBuilder.addMethod(methodBuilder.build());
    }

    util.generateSourceFile(
        resourceTemplatesClassName.canonicalName(),
        JavaFile.builder(
                resourceTemplatesClassName.packageName(), resourceTemplatesClassBuilder.build())
            .build(),
        latticeAppTypeElement);
  }

  private void generateMcpTools(
      LatticeCodegenContext latticeCodegenContext,
      CodeGenUtility util,
      TypeElement latticeAppTypeElement,
      McpServer mcpServer) {
    List<? extends TypeMirror> toolVajrams =
        util.getTypesFromAnnotationMember(mcpServer::toolVajrams).stream().toList();
    ClassName mcpToolsClassName =
        ClassName.get(
            util.getPackageName(latticeAppTypeElement),
            latticeAppTypeElement.getSimpleName().toString() + "_QuarkusMcpTools");

    TypeSpec.Builder mcpToolsClassBuilder =
        util.classBuilder(
                mcpToolsClassName.simpleName(), latticeAppTypeElement.getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addAnnotation(Singleton.class);
    addCommonMembers(mcpToolsClassBuilder);
    for (TypeMirror toolVajram : toolVajrams) {
      VajramInfo vajramInfo = latticeCodegenContext.codeGenUtility().computeVajramInfo(toolVajram);
      MethodSpec.Builder methodBuilder = createUniMethodBuilder(vajramInfo, util);
      AnnotationSpec.Builder toolAnnotation =
          AnnotationSpec.builder(Tool.class)
              .addMember("description", "$S", getDocString(vajramInfo));
      if (supportsStructuredContent(vajramInfo, util)) {
        toolAnnotation.addMember("structuredContent", "$L", true);
      }
      methodBuilder.addAnnotation(toolAnnotation.build());
      for (Entry<String, FacetDetail> entry : vajramInfo.lite().facetDetails().entrySet()) {
        String facetName = entry.getKey();
        FacetDetail facetDetail = entry.getValue();
        if (facetDetail.facetType() != FacetType.INPUT) {
          continue;
        }
        methodBuilder.addParameter(
            ParameterSpec.builder(
                    TypeName.get(facetDetail.dataType().javaModelType(util.processingEnv())),
                    facetName)
                .addAnnotation(
                    AnnotationSpec.builder(ToolArg.class)
                        .addMember(
                            "description",
                            "$S",
                            requireNonNullElse(facetDetail.documentation(), ""))
                        .build())
                .build());
      }
      methodBuilder.addCode(
"""
return $T.createFrom()
    .completionStage(
        mcpServerDopant.executeMcpTool(
          $T._builder()
          $L
          ._build()));
""",
          Uni.class,
          ClassName.get(
              vajramInfo.lite().packageName(), vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX),
          vajramInfo.lite().facetDetails().values().stream()
              .filter(fd -> fd.facetType() == FacetType.INPUT)
              .map(fd -> CodeBlock.of(".$L($L)", fd.name(), fd.name()))
              .collect(CodeBlock.joining("\n")));
      mcpToolsClassBuilder.addMethod(methodBuilder.build());
    }

    util.generateSourceFile(
        mcpToolsClassName.canonicalName(),
        JavaFile.builder(mcpToolsClassName.packageName(), mcpToolsClassBuilder.build()).build(),
        latticeAppTypeElement);
  }

  private void generateMcpResources(
      LatticeCodegenContext latticeCodegenContext,
      CodeGenUtility util,
      TypeElement latticeAppTypeElement,
      McpServer mcpServer) {
    List<? extends TypeMirror> resourceVajrams =
        util.getTypesFromAnnotationMember(mcpServer::resourceVajrams).stream().toList();
    ClassName mcpResourcesClassName =
        ClassName.get(
            util.getPackageName(latticeAppTypeElement),
            latticeAppTypeElement.getSimpleName().toString() + "_QuarkusMcpResources");

    TypeSpec.Builder mcpResourcesClassBuilder =
        util.classBuilder(
                mcpResourcesClassName.simpleName(),
                latticeAppTypeElement.getQualifiedName().toString())
            .addModifiers(PUBLIC)
            .addAnnotation(Singleton.class);
    addCommonMembers(mcpResourcesClassBuilder);
    for (TypeMirror resourceVajram : resourceVajrams) {
      VajramInfo vajramInfo =
          latticeCodegenContext.codeGenUtility().computeVajramInfo(resourceVajram);
      if (!vajramInfo.lite().facetDetails().isEmpty()) {
        util.error(
            "Resource vajrams cannot have input parameters. Use as a Tool or ResourceTemplate instead.");
      }
      MethodSpec.Builder methodBuilder = createUniMethodBuilder(vajramInfo, util);
      methodBuilder.addAnnotation(
          AnnotationSpec.builder(Resource.class)
              .addMember("uri", "$S", "vajram://" + vajramInfo.vajramName())
              .addMember("description", "$S", getDocString(vajramInfo))
              .build());
      methodBuilder.addCode(
"""
return $T.createFrom()
    .completionStage(
        mcpServerDopant.executeMcpResource(
          $T._builder()
          ._build()));
""",
          Uni.class,
          ClassName.get(
              vajramInfo.lite().packageName(),
              vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX));
      mcpResourcesClassBuilder.addMethod(methodBuilder.build());
    }

    util.generateSourceFile(
        mcpResourcesClassName.canonicalName(),
        JavaFile.builder(mcpResourcesClassName.packageName(), mcpResourcesClassBuilder.build())
            .build(),
        latticeAppTypeElement);
  }

  private MethodSpec.Builder createUniMethodBuilder(VajramInfo vajramInfo, CodeGenUtility util) {
    return MethodSpec.methodBuilder(lowerCaseFirstChar(vajramInfo.vajramName()))
        .returns(
            ParameterizedTypeName.get(
                ClassName.get(Uni.class),
                TypeName.get(vajramInfo.lite().responseType().javaModelType(util.processingEnv()))
                    .box()));
  }

  private String getDocString(VajramInfo vajramInfo) {
    return requireNonNullElse(vajramInfo.lite().docString(), "");
  }

  private void addCommonMembers(TypeSpec.Builder classBuilder) {
    classBuilder.addField(McpServerDopant.class, "mcpServerDopant", PRIVATE, Modifier.FINAL);
    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addAnnotation(Inject.class)
            .addModifiers(PUBLIC)
            .addParameter(McpServerDopant.class, "mcpServerDopant")
            .addCode("this.mcpServerDopant = mcpServerDopant;")
            .build());
  }

  private boolean supportsStructuredContent(VajramInfo vajramInfo, CodeGenUtility util) {
    TypeMirror responseType = vajramInfo.lite().responseType().javaModelType(util.processingEnv());
    Types typeUtils = util.processingEnv().getTypeUtils();
    Elements elementUtils = util.processingEnv().getElementUtils();

    TypeElement modelElement =
        elementUtils.getTypeElement(requireNonNull(Model.class.getCanonicalName()));
    if (modelElement == null || !typeUtils.isSubtype(responseType, modelElement.asType())) {
      return false;
    }

    TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
    if (responseTypeElement == null) {
      return false;
    }

    SupportedModelProtocols supportedModelProtocols =
        responseTypeElement.getAnnotation(SupportedModelProtocols.class);

    if (supportedModelProtocols == null) {
      return false;
    }

    return util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(typeUtils::asElement)
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(Json.class.getCanonicalName()::equals);
  }
}
