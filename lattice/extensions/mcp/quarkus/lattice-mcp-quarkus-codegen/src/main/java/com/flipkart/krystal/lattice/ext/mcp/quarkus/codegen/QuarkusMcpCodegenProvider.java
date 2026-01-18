package com.flipkart.krystal.lattice.ext.mcp.quarkus.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_POJO_SUFFIX;
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
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite.FacetDetail;
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
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map.Entry;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.extern.slf4j.Slf4j;

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
      List<? extends TypeMirror> toolVajrams =
          util.getTypesFromAnnotationMember(mcpServer::toolVajrams).stream().toList();
      ClassName mcpToolsClassName =
          ClassName.get(
              util.getPackageName(latticeAppTypeElement),
              latticeAppTypeElement.getSimpleName().toString() + "_McpTools");

      TypeSpec.Builder mcpToolsClassBuilder =
          util.classBuilder(
                  mcpToolsClassName.simpleName(),
                  latticeAppTypeElement.getQualifiedName().toString())
              .addModifiers(PUBLIC)
              .addAnnotation(Singleton.class);
      mcpToolsClassBuilder.addField(
          McpServerDopant.class, "mcpServerDopant", PRIVATE, Modifier.FINAL);
      mcpToolsClassBuilder.addMethod(
          MethodSpec.constructorBuilder()
              .addAnnotation(Inject.class)
              .addModifiers(PUBLIC)
              .addParameter(McpServerDopant.class, "mcpServerDopant")
              .addCode("this.mcpServerDopant = mcpServerDopant;")
              .build());
      for (TypeMirror toolVajram : toolVajrams) {
        VajramInfo vajramInfo =
            latticeCodegenContext.codeGenUtility().computeVajramInfo(toolVajram);
        MethodSpec.Builder methodBuilder =
            MethodSpec.methodBuilder(lowerCaseFirstChar(vajramInfo.vajramName()))
                .addAnnotation(
                    AnnotationSpec.builder(Tool.class)
                        .addMember(
                            "description",
                            "$S",
                            requireNonNullElse(vajramInfo.lite().docString(), ""))
                        .build())
                .returns(
                    ParameterizedTypeName.get(
                        ClassName.get(Uni.class),
                        TypeName.get(
                                vajramInfo
                                    .lite()
                                    .responseType()
                                    .javaModelType(util.processingEnv()))
                            .box()));
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
                vajramInfo.lite().packageName(),
                vajramInfo.vajramName() + IMMUT_REQUEST_POJO_SUFFIX),
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
    };
  }
}
