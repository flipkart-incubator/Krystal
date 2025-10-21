package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_IMMUT_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_POJO_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.IMMUT_REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.WRPR_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeRegistry;
import com.flipkart.krystal.codegen.common.models.CodeGenShortCircuitException;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.models.TypeAndName;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel.DefaultFacetModelBuilder;
import com.flipkart.krystal.vajram.codegen.common.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Actual;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Boxed;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.FanoutResponses;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.One2OneResponse;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.OptionalType;
import com.flipkart.krystal.vajram.codegen.common.models.VajramInfoLite.FacetDetail;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassWithTooManyMethods")
@Slf4j
public class VajramCodeGenUtility {

  private static final ImmutableMap<Class<?>, String> DISALLOWED_FACET_TYPES =
      ImmutableMap.<Class<?>, String>builder()
          .put(
              Optional.class,
              Optional.class
                  + " is not an allowed facet type. All facets are optional by default. So this should not be needed.")
          .put(
              Request.class,
              Request.class
                  + " is not an allowed facet type as this can cause undesired behaviour.")
          .put(
              FacetValues.class,
              FacetValues.class
                  + " is not an allowed facet type as this can cause undesired behaviour.")
          .build();
  public static final Splitter QUALIFIED_FACET_SPLITTER =
      Splitter.onPattern(Constants.QUALIFIED_FACET_SEPARATOR);

  @Getter private final ProcessingEnvironment processingEnv;
  private final Types typeUtils;
  private final Elements elementUtils;
  @Getter private final DataTypeRegistry dataTypeRegistry;
  @Getter private final CodeGenUtility codegenUtil;

  public VajramCodeGenUtility(
      ProcessingEnvironment processingEnv, Class<?> generator, @Nullable String phaseString) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.dataTypeRegistry = new DataTypeRegistry();
    this.codegenUtil = new CodeGenUtility(processingEnv, generator, phaseString);
  }

  public FacetJavaType getFacetReturnType(FacetGenModel facet, CodeGenParams codeGenParams) {
    if (facet instanceof DependencyModel dep) {
      if (dep.canFanout()) {
        return new FanoutResponses(this);
      } else {
        return new One2OneResponse(this);
      }
    } else {
      boolean localDevAccessible = codeGenParams.isDevAccessible() && codeGenParams.isLocal();
      if (localDevAccessible) {
        IfAbsent ifAbsent = codegenUtil.getIfAbsent(facet.facetField());
        // Developers should not deal with boxed types. So we need to return the actual type or
        // an Optional wrapper as needed
        if (ifAbsent.value().isMandatoryOnServer()) {
          return new Actual(this);
        }
        // This means the facet is either conditionally or always optional
        return new OptionalType(this);
      }
      return new Boxed(this);
    }
  }

  public String extractFacetName(
      String vajramId, String qualifiedFacet, ExecutableElement resolverMethod) {
    List<String> parts = QUALIFIED_FACET_SPLITTER.splitToList(qualifiedFacet);
    if (parts.size() != 2) {
      throw codegenUtil.errorAndThrow(
          "Qualified Facet is not of the form <vajramId>:<facetName> : " + qualifiedFacet,
          resolverMethod);
    }
    if (!vajramId.equals(parts.get(0))) {
      codegenUtil()
          .error(
              "Expected vajram id '"
                  + vajramId
                  + "' does not match with the given qualified facet: "
                  + qualifiedFacet,
              resolverMethod);
    }
    return parts.get(1);
  }

  public List<TypeElement> getDefinitionClasses(RoundEnvironment roundEnv) {
    return Stream.concat(
            roundEnv.getElementsAnnotatedWith(Vajram.class).stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .map(executableElement -> (TypeElement) executableElement)
                .filter(typeElement -> typeElement.getModifiers().contains(ABSTRACT)),
            roundEnv.getElementsAnnotatedWith(Trait.class).stream()
                .filter(element -> element.getKind() == ElementKind.INTERFACE)
                .map(executableElement -> (TypeElement) executableElement))
        .toList();
  }

  public VajramInfo computeVajramInfo(TypeElement vajramClass) {
    VajramInfoLite vajramInfoLite = computeVajramInfoLite(vajramClass);
    VajramInfoLite conformsToTraitInfo = getConformToTraitInfoFromVajram(vajramClass);
    Optional<Element> inputsClass =
        vajramClass.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .filter(element -> element.getSimpleName().contentEquals(Constants._INPUTS_CLASS))
            .findFirst()
            .map(element -> typeUtils.asElement(element.asType()));
    Optional<Element> internalFacetsClass =
        vajramClass.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .filter(
                element -> element.getSimpleName().contentEquals(Constants._INTERNAL_FACETS_CLASS))
            .findFirst()
            .map(element -> typeUtils.asElement(element.asType()));
    BiMap<String, Integer> givenIdsByName = HashBiMap.create();
    Set<Integer> takenFacetIds = givenIdsByName.values();
    List<VariableElement> inputFields =
        ElementFilter.fieldsIn(inputsClass.map(Element::getEnclosedElements).orElse(List.of()));
    List<VariableElement> internalFacetFields =
        ElementFilter.fieldsIn(
            internalFacetsClass.map(Element::getEnclosedElements).orElse(List.of()));
    List<VariableElement> dependencyFields =
        internalFacetFields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
            .toList();
    AtomicInteger nextFacetId = new AtomicInteger(1);
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramInfoLite,
            Streams.concat(inputFields.stream(), internalFacetFields.stream())
                .map(
                    inputField ->
                        toGivenFacetModel(
                            inputField, givenIdsByName, takenFacetIds, nextFacetId, vajramInfoLite))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList()),
            dependencyFields.stream()
                .map(
                    depField ->
                        Optional.ofNullable(
                            toDependencyModel(
                                vajramInfoLite,
                                depField,
                                givenIdsByName,
                                takenFacetIds,
                                nextFacetId)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList()),
            conformsToTraitInfo);
    codegenUtil().note("VajramInfo: %s".formatted(vajramInfo));
    validateVajramInfo(vajramInfo);
    return vajramInfo;
  }

  private void validateVajramInfo(VajramInfo vajramInfo) {
    vajramInfo
        .facetStream()
        .forEach(
            facetGenModel -> {
              if (facetGenModel.name().startsWith("_")) {
                @Nullable Element[] elements = new Element[] {facetGenModel.facetField()};
                codegenUtil()
                    .error(
                        "Facet names cannot start with an underscore (_). These are reserved for platform specific identifiers",
                        elements);
              }
            });
  }

  private Optional<DefaultFacetModel> toGivenFacetModel(
      VariableElement facetField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId,
      VajramInfoLite vajramInfoLite) {
    DefaultFacetModelBuilder facetBuilder = DefaultFacetModel.builder().facetField(facetField);
    String facetName = facetField.getSimpleName().toString();
    facetBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    facetBuilder.name(facetName);
    facetBuilder.documentation(elementUtils.getDocComment(facetField));
    TypeMirror facetFieldType = facetField.asType();
    if (TypeKind.ERROR.equals(facetFieldType.getKind())) {
      throw new CodeGenShortCircuitException(
          "Vajram Id : "
              + vajramInfoLite.vajramId()
              + " facet : "
              + facetName
              + " has an error type: "
              + facetFieldType);
    }

    CodeGenType dataType =
        facetFieldType.accept(
            new DeclaredTypeVisitor(codegenUtil, facetField, DISALLOWED_FACET_TYPES), null);
    facetBuilder.dataType(dataType);
    FacetType facetType = null;
    boolean isInput = "_Inputs".contentEquals(facetField.getEnclosingElement().getSimpleName());
    if (isInput) {
      facetType = FacetType.INPUT;
    }
    if (facetField.getAnnotation(Inject.class) != null) {
      if (isInput) {
        codegenUtil()
            .error("Inject facet '%s' cannot be an input facet".formatted(facetName), facetField);
      }
      facetType = INJECTION;
    }
    if (facetType == null) {
      return Optional.empty();
    }
    DefaultFacetModel facetModel =
        facetBuilder.facetType(facetType).vajramInfo(vajramInfoLite).build();
    givenIdsByName.putIfAbsent(facetName, facetModel.id());
    return Optional.of(facetModel);
  }

  private static int getNextAvailableFacetId(
      Set<Integer> takenFacetIds, AtomicInteger nextFacetId) {
    while (takenFacetIds.contains(nextFacetId.get())) {
      nextFacetId.getAndIncrement();
    }
    return nextFacetId.getAndIncrement();
  }

  private @Nullable DependencyModel toDependencyModel(
      VajramInfoLite vajramInfo,
      VariableElement depField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId) {
    VajramID vajramId = vajramInfo.vajramId();
    String facetName = depField.getSimpleName().toString();
    Dependency dependency = depField.getAnnotation(Dependency.class);
    DependencyModelBuilder depBuilder = DependencyModel.builder().facetField(depField);
    depBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    depBuilder.name(facetName);
    Optional<TypeMirror> vajramReqType =
        codegenUtil
            .getTypeFromAnnotationMember(dependency::withVajramReq)
            .filter(
                typeMirror ->
                    !checkNotNull((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            CodeGenUtility.getTypeElement(Request.class.getName(), processingEnv)
                                .getQualifiedName()));
    Optional<TypeMirror> vajramType =
        codegenUtil
            .getTypeFromAnnotationMember(dependency::onVajram)
            .filter(
                typeMirror ->
                    !checkNotNull((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            CodeGenUtility.getTypeElement(VajramDef.class.getName(), processingEnv)
                                .getQualifiedName()));
    TypeMirror vajramOrReqType =
        vajramReqType
            .or(() -> vajramType)
            .orElseThrow(
                () -> {
                  String message =
                      "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                          .formatted(depField.getSimpleName(), vajramId);
                  codegenUtil().error(message, depField);
                  return new VajramDefinitionException("Invalid Dependency specification");
                });
    depBuilder.documentation(elementUtils.getDocComment(depField));
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      String message =
          ("Both `withVajramReq` and `onVajram` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'."
                  + " Found withVajramReq=%s and onVajram=%s")
              .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get());
      codegenUtil().error(message, depField);
    } else {
      CodeGenType declaredDataType =
          depField
              .asType()
              .accept(new DeclaredTypeVisitor(codegenUtil, depField, DISALLOWED_FACET_TYPES), null);
      TypeElement vajramOrReqElement =
          checkNotNull((TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType));
      VajramInfoLite depVajramInfoLite = computeVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramInfo(depVajramInfoLite)
          .depReqClassName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!declaredDataType.equals(depVajramInfoLite.responseType())) {
        codegenUtil()
            .error(
                "Declared dependency type %s does not match dependency vajram response type %s"
                    .formatted(declaredDataType, depVajramInfoLite.responseType()),
                depField);
      }
      DependencyModel depModel =
          depBuilder.dataType(declaredDataType).vajramInfo(vajramInfo).build();
      givenIdsByName.putIfAbsent(facetName, depModel.id());
      return depModel;
    }
    String message =
        ("Invalid dependency spec of dependency '%s' of vajram '%s'."
                + " Found withVajramReq=%s and onVajram=%s")
            .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get());
    codegenUtil().error(message, depField);
    return null;
  }

  public VajramInfoLite computeVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    ImmutableMap<String, FacetDetail> facetIdNameMappings = ImmutableMap.of();
    VajramID vajramId;
    CodeGenType responseType;
    String packageName = elementUtils.getPackageOf(vajramOrReqClass).getQualifiedName().toString();
    @Nullable TypeElement requestType;
    if (codegenUtil().isRawAssignable(vajramOrReqClass.asType(), Request.class)) {
      requestType = vajramOrReqClass;
      TypeMirror responseTypeMirror = getVajramResponseType(vajramOrReqClass, Request.class);
      vajramId =
          vajramID(
              vajramClassSimpleName.substring(
                  0, vajramClassSimpleName.length() - REQUEST_SUFFIX.length()));
      responseType =
          responseTypeMirror.accept(
              new DeclaredTypeVisitor(codegenUtil, vajramOrReqClass, DISALLOWED_FACET_TYPES), null);
    } else if (codegenUtil().isRawAssignable(vajramOrReqClass.asType(), VajramDefRoot.class)) {
      Vajram vajram = vajramOrReqClass.getAnnotation(Vajram.class);
      Trait trait = vajramOrReqClass.getAnnotation(Trait.class);
      if (vajram == null && trait == null) {
        throw codegenUtil.errorAndThrow(
            "Vajram class does not have either @Vajram or @Trait annotation. This should not happen",
            vajramOrReqClass);
      }
      TypeMirror responseTypeMirror = getVajramResponseType(vajramOrReqClass, VajramDefRoot.class);
      requestType =
          elementUtils.getTypeElement(
              packageName + "." + getRequestInterfaceName(vajramClassSimpleName));
      vajramId = getVajramIdOfVajramClass(vajramOrReqClass);
      responseType =
          responseTypeMirror.accept(
              new DeclaredTypeVisitor(codegenUtil, vajramOrReqClass, DISALLOWED_FACET_TYPES), null);
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, VajramDef.class, ImmutableRequest.class));
    }
    return new VajramInfoLite(
        vajramId,
        responseType,
        packageName,
        facetDetailsFromRequestType(requestType),
        vajramOrReqClass,
        codegenUtil.processingEnv().getElementUtils().getDocComment(vajramOrReqClass),
        this);
  }

  private ImmutableMap<String, FacetDetail> facetDetailsFromRequestType(
      @Nullable TypeElement requestType) {
    if (requestType == null) {
      return ImmutableMap.of();
    }
    return ElementFilter.fieldsIn(requestType.getEnclosedElements()).stream()
        .filter(
            element ->
                element.asType() instanceof DeclaredType d
                    && d.asElement() instanceof TypeElement t
                    && t.getQualifiedName().contentEquals(InputMirrorSpec.class.getCanonicalName()))
        .map(
            facetSpecField -> {
              FacetIdNameMapping facetIdNameMapping =
                  facetSpecField.getAnnotation(FacetIdNameMapping.class);
              return new FacetDetail(
                  facetIdNameMapping.id(),
                  facetIdNameMapping.name(),
                  facetSpecField
                      .asType()
                      .accept(
                          new DeclaredTypeVisitor(codegenUtil, requestType, DISALLOWED_FACET_TYPES),
                          null)
                      .typeParameters()
                      .get(0),
                  INPUT,
                  elementUtils.getDocComment(facetSpecField));
            })
        .collect(toImmutableMap(FacetDetail::name, Function.identity()));
  }

  public VajramID getVajramIdOfVajramClass(TypeElement vajramOrReqClass) {
    return vajramID(vajramOrReqClass.getSimpleName().toString());
  }

  private @Nullable VajramInfoLite getConformToTraitInfoFromVajram(TypeElement vajramClass) {
    Optional<TypeElement> conformsToTrait = getConformsToTraitType(vajramClass);
    VajramInfoLite conformsToTraitInfo = null;
    if (conformsToTrait.isPresent()) {
      conformsToTraitInfo = computeVajramInfoLite(conformsToTrait.get());
    }
    return conformsToTraitInfo;
  }

  private Optional<TypeElement> getConformsToTraitType(TypeElement vajramOrReqClass) {
    for (TypeMirror superInterface : vajramOrReqClass.getInterfaces()) {
      Element element = typeUtils.asElement(superInterface);
      if (element instanceof TypeElement typeElement
          && checkNotNull(element).getAnnotation(Trait.class) != null) {
        return Optional.of(typeElement);
      }
    }
    return Optional.empty();
  }

  private ClassName getVajramReqClassName(TypeElement vajramClass) {
    TypeMirror from1 = vajramClass.asType();
    if (codegenUtil().isRawAssignable(from1, VajramDefRoot.class)) {
      return ClassName.get(
          elementUtils.getPackageOf(vajramClass).getQualifiedName().toString(),
          vajramClass.getSimpleName() + REQUEST_SUFFIX);
    } else {
      TypeMirror from = vajramClass.asType();
      if (codegenUtil().isRawAssignable(from, Request.class)) {
        return ClassName.get(vajramClass);
      } else {
        throw new AssertionError("This should not happen! Found:" + vajramClass);
      }
    }
  }

  private TypeMirror getVajramResponseType(TypeElement vajramOrReqType, Class<?> targetClass) {
    int typeParamIndex = 0;
    TypeElement targetParentClass =
        requireNonNull(elementUtils.getTypeElement(requireNonNull(targetClass.getCanonicalName())));
    List<? extends TypeMirror> typeParameters =
        codegenUtil().getTypeParamTypes(vajramOrReqType, targetParentClass);
    if (typeParameters.size() > typeParamIndex) {
      return typeParameters.get(typeParamIndex);
    } else {
      throw codegenUtil.errorAndThrow(
          "Incorrect number of parameter types on Vajram interface. Expected 1, Found %s. Unable to infer response type for Vajram %s"
              .formatted(typeParameters, vajramOrReqType.getQualifiedName()),
          vajramOrReqType);
    }
  }

  public static String getRequestInterfaceName(String vajramName) {
    return vajramName + REQUEST_SUFFIX;
  }

  public static String getImmutRequestInterfaceName(String vajramName) {
    return vajramName + IMMUT_REQUEST_SUFFIX;
  }

  public static String getImmutRequestPojoName(String vajramName) {
    return vajramName + IMMUT_REQUEST_POJO_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + WRPR_SUFFIX;
  }

  public static String getFacetsInterfaceName(String vajramName) {
    return vajramName + FACETS_CLASS_SUFFIX;
  }

  public static String getImmutFacetsClassName(String vajramName) {
    return vajramName + FACETS_IMMUT_CLASS_SUFFIX;
  }

  TypeName responseType(DependencyModel dep) {
    return responseType(
        new TypeAndName(dep.depReqClassName()), codegenUtil().getTypeName(dep.dataType()));
  }

  private TypeName responseType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(One2OneDepResponse.class),
        requestType.typeName(),
        codegenUtil().box(facetType).typeName());
  }

  public TypeName responsesType(DependencyModel dep) {
    return responsesType(
        new TypeAndName(dep.depReqClassName()), codegenUtil().getTypeName(dep.dataType()));
  }

  private TypeName responsesType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(FanoutDepResponses.class),
        requestType.typeName(),
        codegenUtil().box(facetType).typeName());
  }

  public CodeGenType getDataType(FacetGenModel abstractInput) {
    if (abstractInput instanceof DefaultFacetModel facetDef) {
      return facetDef.dataType();
    } else if (abstractInput instanceof DependencyModel dep) {
      return dep.dataType();
    } else {
      throw new UnsupportedOperationException(
          "Unable to extract datatype from facet : %s".formatted(abstractInput));
    }
  }

  public FacetJavaType getFacetFieldType(FacetGenModel facet) {
    if (facet instanceof DependencyModel dep) {
      if (dep.canFanout()) {
        return new FanoutResponses(this);
      } else {
        return new One2OneResponse(this);
      }
    } else {
      return new Boxed(this);
    }
  }

  public boolean usePlatformDefault(FacetGenModel facet) {
    IfAbsent ifAbsent = facet.facetField().getAnnotation(IfAbsent.class);
    return ifAbsent != null && ifAbsent.value().usePlatformDefault();
  }

  public boolean isMandatoryOnServer(FacetGenModel facet) {
    IfAbsent ifAbsent = facet.facetField().getAnnotation(IfAbsent.class);
    return ifAbsent != null && ifAbsent.value().isMandatoryOnServer();
  }

  /**
   * Returns the source output path
   *
   * @param codeGenElement the element for which code gen is being done
   */
  public Path detectSourceOutputPath(@Nullable Element codeGenElement) {
    Path sourcePath;
    try {
      // Create a dummy file to get the location
      FileObject dummyFile =
          processingEnv()
              .getFiler()
              .createResource(
                  StandardLocation.SOURCE_OUTPUT,
                  "",
                  new Random().nextInt() + "_dummy_detect_source_path.txt");
      sourcePath = Paths.get(dummyFile.toUri());
      dummyFile.delete();
    } catch (Exception e) {
      throw codegenUtil.errorAndThrow(
          "Could not detect source output directory because dummy_detect_source_path.txt could not be created",
          codeGenElement);
    }
    return requireNonNull(sourcePath.getParent());
  }

  @SuppressWarnings("unchecked")
  public <T> T getAnnotationElement(
      AnnotationMirror parentModelRootAnno, String annoElement, Class<T> type) {
    return type.cast(
        elementUtils.getElementValuesWithDefaults(parentModelRootAnno).entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().contentEquals(annoElement))
            .findAny()
            .map(Entry::getValue)
            .orElseThrow(AssertionError::new)
            .getValue());
  }
}
