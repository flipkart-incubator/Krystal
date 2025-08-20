package com.flipkart.krystal.lattice.rest.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.rest.RestService;
import com.flipkart.krystal.lattice.rest.RestServiceDopant;
import com.flipkart.krystal.lattice.rest.api.Body;
import com.flipkart.krystal.lattice.rest.api.Path;
import com.flipkart.krystal.lattice.rest.api.PathParam;
import com.flipkart.krystal.lattice.rest.api.QueryParam;
import com.flipkart.krystal.lattice.rest.api.methods.RestMethod;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.vajram.codegen.common.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfo;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(LatticeCodeGeneratorProvider.class)
public class JakartaRestServiceResourceGenProvider implements LatticeCodeGeneratorProvider {

  @Override
  public CodeGenerator create(LatticeCodegenContext latticeCodegenContext) {
    return new JakartaRestServiceResourceGen(latticeCodegenContext);
  }

  public static List<ClassName> resourceClasses(LatticeCodegenContext context) {
    TypeElement latticeAppElem = context.latticeAppTypeElement();
    List<ClassName> resourceClasses = new ArrayList<>();
    CodeGenUtility util = context.codeGenUtility().codegenUtil();
    for (TypeElement vajramElem :
        JakartaRestServiceResourceGen.getRestResourceVajramElems(latticeAppElem, util)) {
      Path path = vajramElem.getAnnotation(Path.class);
      if (path == null) {
        continue;
      }
      if (path.value().startsWith("/") || path.value().endsWith("/")) {
        util.error("Path value in @Path annotation cannot start of end with '/'", vajramElem);
      }
      VajramInfo vajramInfo = context.codeGenUtility().computeVajramInfo(vajramElem);
      ClassName jaxRsResourceName = getJaxRsResourceName(vajramInfo, vajramElem);
      resourceClasses.add(jaxRsResourceName);
    }
    return resourceClasses;
  }

  private static class JakartaRestServiceResourceGen implements CodeGenerator {

    private final LatticeCodegenContext context;

    private final CodeGenUtility util;

    public JakartaRestServiceResourceGen(LatticeCodegenContext context) {
      this.context = context;
      this.util = context.codeGenUtility().codegenUtil();
    }

    @Override
    public void generate() {
      if (!isApplicable()) {
        return;
      }
      TypeElement latticeAppElem = context.latticeAppTypeElement();
      jakartaResources(latticeAppElem);
      dopantImpl(latticeAppElem);
    }

    private void jakartaResources(TypeElement latticeAppElem) {
      RestService restService = latticeAppElem.getAnnotation(RestService.class);
      String pathPrefix = restService.pathPrefix().isEmpty() ? "" : "/" + restService.pathPrefix();
      for (TypeElement vajramElem : getRestResourceVajramElems(latticeAppElem, util)) {
        VajramInfo vajramInfo = context.codeGenUtility().computeVajramInfo(vajramElem);
        var resourceMethods = resourceMethods(vajramElem, vajramInfo);
        if (resourceMethods.isEmpty()) {
          continue;
        }
        Path path = vajramElem.getAnnotation(Path.class);
        String pathValue;
        if (path == null) {
          pathValue = vajramInfo.vajramName();
        } else {
          pathValue = path.value();
        }
        ClassName jaxRsResourceName = getJaxRsResourceName(vajramInfo, vajramElem);
        TypeSpec.Builder resourceClassBuilder =
            util.classBuilder(jaxRsResourceName.simpleName()).addModifiers(PUBLIC);
        resourceClassBuilder
            .addField(RestServiceDopant.class, "_restServiceDopant", PRIVATE, FINAL)
            .addAnnotation(
                AnnotationSpec.builder(jakarta.ws.rs.Path.class)
                    .addMember("value", "$S", pathPrefix + "/" + pathValue)
                    .build())
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(RestServiceDopant.class, "_restServiceDopant")
                    .addStatement("this._restServiceDopant = _restServiceDopant")
                    .build())
            .addMethods(resourceMethods);

        util.generateSourceFile(
            jaxRsResourceName.canonicalName(),
            JavaFile.builder(jaxRsResourceName.packageName(), resourceClassBuilder.build()).build(),
            latticeAppElem);
      }
    }

    private static List<TypeElement> getRestResourceVajramElems(
        TypeElement latticeAppElem, CodeGenUtility util) {
      List<TypeElement> restResourceVajramElems =
          util
              .getTypesFromAnnotationMember(
                  latticeAppElem.getAnnotation(RestService.class)::resourceVajrams)
              .stream()
              .map(
                  typeMirror ->
                      requireNonNull(
                          (TypeElement) util.processingEnv().getTypeUtils().asElement(typeMirror)))
              .toList();
      return restResourceVajramElems;
    }

    @SneakyThrows
    private void dopantImpl(TypeElement latticeAppElem) {
      List<ClassName> resourceClassNames = resourceClasses(context);

      String packageName =
          util.processingEnv()
              .getElementUtils()
              .getPackageOf(latticeAppElem)
              .getQualifiedName()
              .toString();
      LatticeCodegenUtils latticeCodegenUtils = new LatticeCodegenUtils(util);
      ClassName dopantImplName =
          latticeCodegenUtils.getDopantImplName(latticeAppElem, RestServiceDopant.class);
      TypeSpec.Builder dopantImplBuilder =
          util.classBuilder(dopantImplName.simpleName())
              .addModifiers(Modifier.FINAL)
              .superclass(RestServiceDopant.class)
              .addMethod(
                  latticeCodegenUtils.dopantConstructorOverride(RestServiceDopant.class).build());
      if (!resourceClassNames.isEmpty()) {
        dopantImplBuilder.addMethod(
            MethodSpec.overriding(
                    util.getMethod(
                        RestServiceDopant.class,
                        RestServiceDopant.class.getMethod("getResources").getName(),
                        0))
                .addStatement(
                    "return $T.of($L)",
                    List.class,
                    resourceClassNames.stream()
                        .map(rc -> CodeBlock.of("new $T(this)", rc))
                        .collect(CodeBlock.joining(",\n")))
                .build());
      }
      util.generateSourceFile(
          dopantImplName.canonicalName(),
          JavaFile.builder(packageName, dopantImplBuilder.build()).build(),
          latticeAppElem);
    }

    private List<MethodSpec> resourceMethods(TypeElement vajramElem, VajramInfo vajramInfo) {
      List<MethodSpec.Builder> resourceMethods = new ArrayList<>();
      @Nullable RestMethod jaxRsRestMethod = getJaxRsRestMethod(vajramInfo);
      if (jaxRsRestMethod == null) {
        jaxRsRestMethod = RestMethod.POST;
      }
      boolean explicitPath = vajramElem.getAnnotation(Path.class) != null;
      Map<String, SerdeProtocol> configProviders =
          ServiceLoader.load(ModelProtocolConfigProvider.class, this.getClass().getClassLoader())
              .stream()
              .map(ServiceLoader.Provider::get)
              .map(ModelProtocolConfigProvider::getConfig)
              .map(ModelProtocolConfig::serdeProtocol)
              .collect(
                  Collectors.toMap(
                      c -> requireNonNull(c.getClass().getCanonicalName()), Function.identity()));

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(lowerCaseFirstChar(vajramInfo.vajramName()))
              .addAnnotation(jaxRsRestMethod.jakartaAnnotation())
              .addModifiers(PUBLIC)
              .addParameter(
                  ParameterSpec.builder(AsyncResponse.class, "_asyncResponse")
                      .addAnnotation(Suspended.class)
                      .build())
              .addParameter(
                  ParameterSpec.builder(HttpHeaders.class, "_httpHeaders")
                      .addAnnotation(Context.class)
                      .build())
              .addParameter(
                  ParameterSpec.builder(UriInfo.class, "_uriInfo")
                      .addAnnotation(Context.class)
                      .build());

      Map<FacetGenModel, FacetParamType> params = new LinkedHashMap<>();
      FacetGenModel bodyFacet = null;
      for (FacetGenModel facet : vajramInfo.facetStream().toList()) {
        VariableElement facetField = facet.facetField();
        if (facetField.getAnnotation(PathParam.class) != null) {
          if (!explicitPath) {
            util.error("Path param cannot be used without @Path annotation", facetField);
          } else {
            assignTypeToFacet(facet, FacetParamType.PATH, params);
          }
        }
        if (facetField.getAnnotation(QueryParam.class) != null) {
          if (!explicitPath) {
            util.error("Query param cannot be used without @Path annotation", facetField);
          } else {
            assignTypeToFacet(facet, FacetParamType.QUERY, params);
          }
        }
        if (facetField.getAnnotation(Body.class) != null) {
          if (!explicitPath) {
            util.error("Body param cannot be used without @Path annotation", facetField);
          } else {
            assignTypeToFacet(facet, FacetParamType.BODY, params);
            bodyFacet = facet;
          }
        }
      }

      ImmutableList<TypeElement> requestSerdeProtocols = ImmutableList.of();
      for (Entry<FacetGenModel, FacetParamType> entry : params.entrySet()) {
        FacetGenModel facet = entry.getKey();
        FacetParamType facetParamType = entry.getValue();
        TypeMirror facetType = facet.dataType().javaModelType(util.processingEnv());
        if (facetParamType == FacetParamType.PATH) {
          methodBuilder.addParameter(
              ParameterSpec.builder(TypeName.get(facetType), facet.name())
                  .addAnnotation(
                      AnnotationSpec.builder(jakarta.ws.rs.PathParam.class)
                          .addMember("value", "$S", facet.name())
                          .build())
                  .build());
        } else if (facetParamType == FacetParamType.QUERY) {
          methodBuilder.addParameter(
              ParameterSpec.builder(TypeName.get(facetType), facet.name())
                  .addAnnotation(
                      AnnotationSpec.builder(jakarta.ws.rs.QueryParam.class)
                          .addMember("value", "$S", facet.name())
                          .build())
                  .build());
        } else if (facetParamType == FacetParamType.BODY) {
          Element bodyTypeElem =
              requireNonNull(util.processingEnv().getTypeUtils().asElement(facetType));
          SupportedModelProtocols supportedModelProtocols =
              bodyTypeElem.getAnnotation(SupportedModelProtocols.class);
          if (supportedModelProtocols == null) {
            util.error(
                "Rest request Body facet " + facet.name() + " doesn't support any ModelProtocol.",
                facet.facetField());
          } else {
            requestSerdeProtocols =
                util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
                    .filter(t -> util.isRawAssignable(t, SerdeProtocol.class))
                    .<@NonNull TypeElement>map(
                        typeMirror ->
                            requireNonNull(
                                (TypeElement)
                                    util.processingEnv().getTypeUtils().asElement(typeMirror)))
                    .collect(toImmutableList());
            if (requestSerdeProtocols.isEmpty()) {
              util.error(
                  "Rest request Body facet "
                      + facet.name()
                      + " doesn't support any SerdeProtocols. Found: "
                      + Arrays.toString(supportedModelProtocols.value()),
                  facet.facetField());
            }
          }
        }
      }
      if (bodyFacet != null || !explicitPath) {
        SupportedModelProtocols supportedModelProtocols;
        if (bodyFacet != null) {
          TypeMirror facetType = bodyFacet.dataType().javaModelType(util.processingEnv());
          Element bodyTypeElem =
              requireNonNull(util.processingEnv().getTypeUtils().asElement(facetType));
          supportedModelProtocols = bodyTypeElem.getAnnotation(SupportedModelProtocols.class);
        } else {
          supportedModelProtocols = vajramElem.getAnnotation(SupportedModelProtocols.class);
        }
        if (supportedModelProtocols == null) {
          util.error(
              "Rest request body doesn't support any ModelProtocol.",
              bodyFacet == null ? vajramElem : bodyFacet.facetField());
        } else {
          requestSerdeProtocols =
              util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
                  .filter(t -> util.isRawAssignable(t, SerdeProtocol.class))
                  .map(
                      typeMirror ->
                          requireNonNull(
                              (TypeElement)
                                  util.processingEnv().getTypeUtils().asElement(typeMirror)))
                  .collect(toImmutableList());
          if (requestSerdeProtocols.isEmpty()) {
            util.error(
                "Rest request Body facet doesn't support any SerdeProtocols. Found: "
                    + Arrays.toString(supportedModelProtocols.value()),
                bodyFacet == null ? vajramElem : bodyFacet.facetField());
          }
        }
      }
      if (bodyFacet != null || explicitPath) {
        if (bodyFacet != null) {
          methodBuilder.addStatement(
              """
            var _vajramRequest = $T._builder()
              $L
            """,
              vajramInfo.lite().immutReqPojoType(),
              params.keySet().stream()
                  .filter(p -> params.get(p) != FacetParamType.BODY)
                  .map(
                      facet ->
                          CodeBlock.builder().add(".$L($L)", facet.name(), facet.name()).build())
                  .collect(CodeBlock.joining("\n")));
          if (!jaxRsRestMethod.supportsRequestBody()) {
            util.error(
                """
                  Vajram %s is mapped to the rest method %s which \
                  doesn't support request body, but the vajram has a \
                  facet with the @Body annotation."""
                    .formatted(vajramInfo.vajramName(), jaxRsRestMethod),
                bodyFacet.facetField());
          }
        }
        TypeElement requestClass = null;
        if (bodyFacet != null) {
          requestClass =
              requireNonNull(
                  (TypeElement)
                      util.processingEnv()
                          .getTypeUtils()
                          .asElement(bodyFacet.dataType().javaModelType(util.processingEnv())));
        }
        for (TypeElement serdeProtocolType : requestSerdeProtocols) {
          SerdeProtocol serdeProtocol =
              configProviders.get(serdeProtocolType.getQualifiedName().toString());
          if (serdeProtocol == null) {
            continue;
          }
          MethodSpec.Builder serdeSpecificMethodBuilder =
              methodBuilder.build().toBuilder()
                  .setName(
                      lowerCaseFirstChar(vajramInfo.vajramName())
                          + "_"
                          + serdeProtocol.modelClassesSuffix());

          serdeSpecificMethodBuilder
              .addAnnotation(
                  AnnotationSpec.builder(Consumes.class)
                      .addMember("value", "$S", serdeProtocol.contentType())
                      .build())
              .addParameter(
                  ParameterSpec.builder(
                          byte[].class, bodyFacet == null ? "_body" : bodyFacet.name())
                      .build());

          if (bodyFacet != null) {
            serdeSpecificMethodBuilder.addStatement(
                CodeBlock.of(
                    "_vajramRequest.$L(new $T($L))",
                    bodyFacet.name(),
                    util.getImmutSerdeClassName(requestClass, serdeProtocol),
                    bodyFacet.name()));
          } else {
            serdeSpecificMethodBuilder.addStatement(
                CodeBlock.of(
                    "var _vajramRequest = new $T(_body)",
                    util.getImmutSerdeClassName(
                        util.processingEnv()
                            .getElementUtils()
                            .getTypeElement(
                                vajramInfo.lite().requestInterfaceType().canonicalName()),
                        serdeProtocol)));
          }
          resourceMethods.add(serdeSpecificMethodBuilder);
        }
      } else {
        resourceMethods.add(methodBuilder);
      }
      return resourceMethods.stream()
          .map(
              r ->
                  r.addStatement(
                      """
                          this._restServiceDopant
                            .executeHttpRequest(_vajramRequest._build(), _httpHeaders, _uriInfo)
                            .whenComplete((_result, _error) -> {
                              if (_error != null) {
                                _asyncResponse.resume(_error);
                              } else {
                                _asyncResponse.resume(_result);
                              }
                            })
                      """))
          .map(MethodSpec.Builder::build)
          .toList();
    }

    private @Nullable RestMethod getJaxRsRestMethod(VajramInfo vajramInfo) {
      TypeElement typeElement = vajramInfo.vajramClassElem();
      for (RestMethod restMethod : RestMethod.values()) {
        Annotation annotation = typeElement.getAnnotation(restMethod.latticeAnnotation());
        if (annotation != null) {
          return restMethod;
        }
      }
      return null;
    }

    enum FacetParamType {
      PATH,
      QUERY,
      BODY;
    }

    private void assignTypeToFacet(
        FacetGenModel facet, FacetParamType type, Map<FacetGenModel, FacetParamType> params) {
      FacetParamType facetParamType = params.get(facet);
      if (facetParamType != type) {
        util.error(
            "The facet " + facet.name() + " cannot be both " + facetParamType + " and " + type,
            facet.facetField());
      } else {
        params.put(facet, type);
      }
    }

    private boolean isApplicable() {
      if (!CodegenPhase.FINAL.equals(context.codegenPhase())) {
        util.note("Skipping Lattice App Impl codegen current phase is not FINAL");
        return false;
      }
      return true;
    }
  }

  public static ClassName getJaxRsResourceName(VajramInfo vajramInfo, TypeElement vajramElem) {
    return ClassName.get(
        vajramInfo.lite().packageName(),
        vajramElem.getSimpleName().toString() + "_JakartaRestResource");
  }
}
