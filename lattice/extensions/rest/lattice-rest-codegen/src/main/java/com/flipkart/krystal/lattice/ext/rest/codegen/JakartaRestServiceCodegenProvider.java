package com.flipkart.krystal.lattice.ext.rest.codegen;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.lowerCaseFirstChar;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenUtils;
import com.flipkart.krystal.lattice.codegen.spi.LatticeCodeGeneratorProvider;
import com.flipkart.krystal.lattice.ext.rest.RestService;
import com.flipkart.krystal.lattice.ext.rest.RestServiceDopant;
import com.flipkart.krystal.lattice.ext.rest.api.Body;
import com.flipkart.krystal.lattice.ext.rest.api.Path;
import com.flipkart.krystal.lattice.ext.rest.api.PathParam;
import com.flipkart.krystal.lattice.ext.rest.api.QueryParam;
import com.flipkart.krystal.lattice.ext.rest.api.methods.RestMethod;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeConfig;
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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@AutoService(LatticeCodeGeneratorProvider.class)
public class JakartaRestServiceCodegenProvider implements LatticeCodeGeneratorProvider {

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
      if (path != null) {
        if (path.value().startsWith("/") || path.value().endsWith("/")) {
          util.error("Path value in @Path annotation cannot start or end with '/'", vajramElem);
        }
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
            util.classBuilder(
                    jaxRsResourceName.simpleName(), latticeAppElem.getQualifiedName().toString())
                .addModifiers(PUBLIC);
        resourceClassBuilder
            .addField(RestServiceDopant.class, "_restServiceDopant", PRIVATE, FINAL)
            .addAnnotation(
                AnnotationSpec.builder(jakarta.ws.rs.Path.class)
                    .addMember("value", "$S", pathPrefix + pathValue)
                    .build())
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addAnnotation(Inject.class)
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
              .<@NonNull TypeElement>map(
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
          util.classBuilder(
                  dopantImplName.simpleName(), latticeAppElem.getQualifiedName().toString())
              .addAnnotation(Singleton.class)
              .addModifiers(FINAL, PUBLIC)
              .superclass(RestServiceDopant.class)
              .addMethod(
                  latticeCodegenUtils
                      .constructorOverride(RestServiceDopant.class)
                      .addModifiers(PUBLIC)
                      .build());
      if (!resourceClassNames.isEmpty()) {
        dopantImplBuilder.addMethod(
            MethodSpec.overriding(
                    util.getMethod(
                        RestServiceDopant.class,
                        RestServiceDopant.class
                            .getDeclaredMethod("declaredApplicationResources")
                            .getName(),
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
      @Nullable RestMethod restMethod = getRestMethod(vajramInfo);
      if (restMethod == null) {
        restMethod = RestMethod.POST;
      }
      boolean explicitPath = vajramElem.getAnnotation(Path.class) != null;
      Map<String, SerdeProtocol> configProviders =
          ServiceLoader.load(ModelProtocolConfigProvider.class, this.getClass().getClassLoader())
              .stream()
              .map(ServiceLoader.Provider::get)
              .map(ModelProtocolConfigProvider::getConfig)
              .map(ModelProtocolConfig::serdeProtocol)
              .collect(
                  toMap(c -> requireNonNull(c.getClass().getCanonicalName()), Function.identity()));
      CodeGenType vajramResponseType = vajramInfo.lite().responseType();
      List<? extends AnnotationMirror> annotations = vajramInfo.lite().annotations();
      Optional<? extends AnnotationMirror> producesAnnotation =
          annotations.stream()
              .filter(
                  a -> a.getAnnotationType().toString().equals(Produces.class.getCanonicalName()))
              .findAny();

      ProcessingEnvironment processingEnv = context.codeGenUtility().processingEnv();
      TypeName jakartaResourceReturnType;
      boolean returnsPublisher;
      WildcardTypeName wildCard = WildcardTypeName.subtypeOf(Object.class);

      ParameterizedTypeName publisherType =
          ParameterizedTypeName.get(ClassName.get(Publisher.class), wildCard);
      if (context
          .codeGenUtility()
          .codegenUtil()
          .isRawAssignable(
              vajramResponseType.rawType().javaModelType(processingEnv), Publisher.class)) {
        jakartaResourceReturnType = publisherType;
        returnsPublisher = true;
      } else {
        jakartaResourceReturnType =
            ParameterizedTypeName.get(ClassName.get(CompletionStage.class), wildCard);
        returnsPublisher = false;
      }

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(lowerCaseFirstChar(vajramInfo.vajramName()))
              .addAnnotation(restMethod.jakartaAnnotation())
              .addModifiers(PUBLIC)
              .returns(jakartaResourceReturnType)
              .addParameter(
                  ParameterSpec.builder(HttpHeaders.class, "_httpHeaders")
                      .addAnnotation(Context.class)
                      .build())
              .addParameter(
                  ParameterSpec.builder(UriInfo.class, "_uriInfo")
                      .addAnnotation(Context.class)
                      .build());
      producesAnnotation.map(AnnotationSpec::get).ifPresent(methodBuilder::addAnnotation);

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
        }
      }
      if (explicitPath) {
        methodBuilder.addStatement(
            """
            var _vajramRequest = $T._builder()
              $L
            """,
            vajramInfo.lite().reqImmutPojoClassName(),
            params.keySet().stream()
                .filter(p -> params.get(p) != FacetParamType.BODY)
                .map(
                    facet -> CodeBlock.builder().add(".$L($L)", facet.name(), facet.name()).build())
                .collect(CodeBlock.joining("\n")));
      }
      if (
      // The vajram has a facet with an explicit @Body annotation; which means we need to
      // deserialize the http body into this facet
      bodyFacet != null
          ||
          // The vajram does not have explicit path - which means we need to deserialize the http
          // body into the vajram request object
          !explicitPath) {

        SupportedModelProtocols supportedModelProtocols;
        Map<Element, SerdeConfig> serdeConfigsMap = new HashMap<>();
        if (bodyFacet != null) {
          Map<@NonNull Element, SerdeConfig> collect =
              Arrays.stream(bodyFacet.facetField().getAnnotationsByType(SerdeConfig.class))
                  .collect(
                      toMap(
                          s ->
                              requireNonNull(
                                  util.processingEnv()
                                      .getTypeUtils()
                                      .asElement(util.getTypeFromAnnotationMember(s::protocol))),
                          s -> s));
          serdeConfigsMap.putAll(collect);
          Element bodyTypeElem =
              requireNonNull(
                  util.processingEnv()
                      .getTypeUtils()
                      .asElement(bodyFacet.dataType().javaModelType(util.processingEnv())));
          for (SerdeConfig serdeConfig : bodyTypeElem.getAnnotationsByType(SerdeConfig.class)) {
            serdeConfigsMap.putIfAbsent(
                requireNonNull(
                    util.processingEnv()
                        .getTypeUtils()
                        .asElement(util.getTypeFromAnnotationMember(serdeConfig::protocol))),
                serdeConfig);
          }
          supportedModelProtocols = bodyTypeElem.getAnnotation(SupportedModelProtocols.class);
        } else {
          supportedModelProtocols = vajramElem.getAnnotation(SupportedModelProtocols.class);
          Map<@NonNull Element, SerdeConfig> collect =
              Arrays.stream(vajramElem.getAnnotationsByType(SerdeConfig.class))
                  .collect(
                      toMap(
                          s ->
                              requireNonNull(
                                  util.processingEnv()
                                      .getTypeUtils()
                                      .asElement(util.getTypeFromAnnotationMember(s::protocol))),
                          s -> s));
          serdeConfigsMap.putAll(collect);
        }
        ImmutableList<TypeElement> requestSerdeProtocols = ImmutableList.of();
        if (supportedModelProtocols == null) {
          util.error(
              "Rest request body doesn't support any ModelProtocol.",
              bodyFacet == null ? vajramElem : bodyFacet.facetField());
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
                "Rest request Body facet doesn't support any SerdeProtocols. Found: "
                    + Arrays.toString(supportedModelProtocols.value()),
                bodyFacet == null ? vajramElem : bodyFacet.facetField());
          }
        }
        if (bodyFacet != null) {
          if (!restMethod.supportsRequestBody()) {
            util.error(
                """
                  Vajram %s is mapped to the rest method %s which \
                  doesn't support request body, but the vajram has a \
                  facet with the @Body annotation."""
                    .formatted(vajramInfo.vajramName(), restMethod),
                bodyFacet.facetField());
          }
        }
        for (TypeElement serdeProtocolType : requestSerdeProtocols) {
          SerdeProtocol serdeProtocol =
              configProviders.get(serdeProtocolType.getQualifiedName().toString());
          if (serdeProtocol == null) {
            continue;
          }
          String[] contentTypes;
          SerdeConfig serdeConfig = serdeConfigsMap.get(serdeProtocolType);
          if (serdeConfig != null) {
            contentTypes = serdeConfig.contentTypes();
          } else {
            contentTypes = new String[] {serdeProtocol.defaultContentType()};
          }
          MethodSpec.Builder serdeSpecificMethodBuilder =
              methodBuilder.build().toBuilder()
                  .setName(
                      lowerCaseFirstChar(vajramInfo.vajramName())
                          + "_"
                          + serdeProtocol.modelClassesSuffix())
                  .returns( // Need to set this again because setName sets return type to Void
                      jakartaResourceReturnType);

          serdeSpecificMethodBuilder
              .addAnnotation(
                  AnnotationSpec.builder(Consumes.class)
                      .addMember(
                          "value",
                          "{$L}",
                          Stream.concat(Arrays.stream(contentTypes), Stream.of("*/*"))
                              .map(c -> CodeBlock.of("$S", c))
                              .collect(CodeBlock.joining(", ")))
                      .build())
              .addParameter(
                  ParameterSpec.builder(
                          byte[].class, bodyFacet == null ? "_body" : bodyFacet.name())
                      .build());

          if (bodyFacet != null) {
            TypeElement bodyFacetModelType =
                requireNonNull(
                    (TypeElement)
                        util.processingEnv()
                            .getTypeUtils()
                            .asElement(bodyFacet.dataType().javaModelType(util.processingEnv())));

            serdeSpecificMethodBuilder.addStatement(
                CodeBlock.of(
                    "_vajramRequest.$L(new $T($L))",
                    bodyFacet.name(),
                    util.getImmutSerdeClassName(bodyFacetModelType, serdeProtocol),
                    bodyFacet.name()));
          } else {
            serdeSpecificMethodBuilder.addStatement(
                CodeBlock.of(
                    "var _vajramRequest = new $T(_body)",
                    util.getImmutSerdeClassName(
                        requireNonNull(
                            util.processingEnv()
                                .getElementUtils()
                                .getTypeElement(
                                    vajramInfo.lite().requestInterfaceClassName().canonicalName())),
                        serdeProtocol)));
          }
          resourceMethods.add(serdeSpecificMethodBuilder);
        }
      } else {
        // This means the vajram does not have a body facet but has an explicit path. This means
        // that this http request doesn't support http body, and we don't need to deserialize
        // anything from the request body.
        resourceMethods.add(methodBuilder);
      }
      return resourceMethods.stream()
          .peek(
              r -> {
                r.addStatement(
                    """
                          $T _completionStage = _restServiceDopant
                            .executeHttpRequest(_vajramRequest._build(), _httpHeaders, _uriInfo)
                      """,
                    ParameterizedTypeName.get(
                        ClassName.get(CompletionStage.class),
                        returnsPublisher ? publisherType : wildCard));
                if (returnsPublisher) {
                  r.addStatement(
                      """
                        $T _publisher = _restServiceDopant.toPublisher(_completionStage)
                  """,
                      publisherType);
                  r.addStatement("return _publisher");
                } else {
                  r.addStatement("return _completionStage");
                }
              })
          .map(MethodSpec.Builder::build)
          .toList();
    }

    private @Nullable RestMethod getRestMethod(VajramInfo vajramInfo) {
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
      if (facetParamType != null && facetParamType != type) {
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
