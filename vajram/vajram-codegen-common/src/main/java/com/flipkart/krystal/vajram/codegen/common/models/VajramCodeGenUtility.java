package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.asTypeNameWithTypes;
import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_IMMUT_POJO_CLASS_SUFFIX;
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

import com.flipkart.krystal.annos.ComputeDelegationMode;
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
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.IOVajramDef;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
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
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

  public VajramCodeGenUtility(CodeGenUtility codegenUtil) {
    this.codegenUtil = codegenUtil;
    this.processingEnv = codegenUtil.processingEnv();
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.dataTypeRegistry = new DataTypeRegistry();
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
        IfAbsent ifAbsent = codegenUtil.getIfAbsent(facet.facetElement(), null);
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

  public VajramInfo computeVajramInfo(TypeMirror vajramClassMirror) {
    TypeElement vajramClass =
        requireNonNull(
            (TypeElement) processingEnv().getTypeUtils().asElement(vajramClassMirror),
            "Could not find vajram class for vajram type " + vajramClassMirror);
    return computeVajramInfo(vajramClass);
  }

  public VajramInfo computeVajramInfo(TypeElement vajramClass) {
    VajramInfoLite vajramInfoLite = computeVajramInfoLiteWithExactTypeArgs(vajramClass.asType());
    VajramInfoLite conformsToTraitInfo = getConformToTraitInfoFromVajram(vajramClass);
    String parentClassName = null;
    if (vajramInfoLite.isVajram()) {
      // All vajrams must have a parent class
      parentClassName =
          ((QualifiedNameable)
                  codegenUtil.processingEnv().getTypeUtils().asElement(vajramClass.getSuperclass()))
              .getQualifiedName()
              .toString();
    }
    Optional<Element> inputsClass = getInputsClass(vajramClass);
    Optional<Element> internalFacetsClass = getInternalFacetsClass(vajramClass);
    BiMap<String, Integer> givenIdsByName = HashBiMap.create();
    Set<Integer> takenFacetIds = givenIdsByName.values();
    List<Element> inputFacetElements = extractFacetElements(inputsClass.orElse(null));
    List<Element> internalFacetElements = extractFacetElements(internalFacetsClass.orElse(null));
    List<Element> dependencyElements =
        internalFacetElements.stream()
            .filter(element -> element.getAnnotation(Dependency.class) != null)
            .toList();
    AtomicInteger nextFacetId = new AtomicInteger(1);
    ComputeDelegationMode outputLogicDelegationMode;
    if (vajramInfoLite.isTrait()) {
      // Traits don't have output logic, so there is not outputLogicDelegationMode
      outputLogicDelegationMode = null;
    } else if (IOVajramDef.class.getCanonicalName().equals(parentClassName)) {
      outputLogicDelegationMode = ComputeDelegationMode.SYNC;
    } else if (ComputeVajramDef.class.getCanonicalName().equals(parentClassName)) {
      outputLogicDelegationMode = ComputeDelegationMode.NONE;
    } else {
      throw codegenUtil.errorAndThrow(
          "Unknown vajram parent class " + parentClassName, vajramClass);
    }
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramInfoLite,
            Streams.concat(inputFacetElements.stream(), internalFacetElements.stream())
                .map(
                    facetElement ->
                        toGivenFacetModel(
                            facetElement,
                            givenIdsByName,
                            takenFacetIds,
                            nextFacetId,
                            vajramInfoLite))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList()),
            dependencyElements.stream()
                .map(
                    depElement ->
                        Optional.ofNullable(
                            toDependencyModel(
                                vajramInfoLite,
                                depElement,
                                givenIdsByName,
                                takenFacetIds,
                                nextFacetId)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList()),
            vajramClass,
            conformsToTraitInfo,
            outputLogicDelegationMode,
            inputsClass.orElse(null));
    codegenUtil().note("VajramInfo: %s".formatted(vajramInfo));
    validateVajramInfo(vajramInfo);
    return vajramInfo;
  }

  private Optional<Element> getInternalFacetsClass(TypeElement vajramClass) {
    return vajramClass.getEnclosedElements().stream()
        .filter(
            element ->
                element.getKind() == ElementKind.CLASS
                    || element.getKind() == ElementKind.INTERFACE)
        .filter(element -> element.getSimpleName().contentEquals(Constants._INTERNAL_FACETS_CLASS))
        .findFirst()
        .map(element -> typeUtils.asElement(element.asType()));
  }

  private Optional<Element> getInputsClass(TypeElement vajramClass) {
    return vajramClass.getEnclosedElements().stream()
        .filter(
            element ->
                element.getKind() == ElementKind.CLASS
                    || element.getKind() == ElementKind.INTERFACE)
        .filter(element -> element.getSimpleName().contentEquals(Constants._INPUTS_CLASS))
        .findFirst()
        .map(element -> typeUtils.asElement(element.asType()));
  }

  private void validateVajramInfo(VajramInfo vajramInfo) {
    vajramInfo
        .facetStream()
        .forEach(
            facetGenModel -> {
              if (facetGenModel.name().startsWith("_")) {
                @Nullable Element[] elements = new Element[] {facetGenModel.facetElement()};
                codegenUtil()
                    .error(
                        "Facet names cannot start with an underscore (_). These are reserved for platform specific identifiers",
                        elements);
              }
            });
  }

  /**
   * Extracts facet-defining elements from a container (class or interface). For classes, returns
   * fields. For interfaces, returns abstract methods. For classes with abstract methods (abstract
   * classes), returns both fields and abstract methods.
   */
  private List<Element> extractFacetElements(@Nullable Element container) {
    if (container == null) {
      return List.of();
    }
    List<? extends Element> enclosed = container.getEnclosedElements();
    List<Element> result = new ArrayList<>();
    // Add fields only for class containers (not interfaces, to exclude public static final
    // constants)
    if (container instanceof TypeElement typeElement
        && typeElement.getKind() == ElementKind.CLASS) {
      result.addAll(ElementFilter.fieldsIn(enclosed));
    }
    // Add abstract methods (from interfaces or abstract classes)
    for (ExecutableElement method : ElementFilter.methodsIn(enclosed)) {
      if (method.getModifiers().contains(ABSTRACT)
          && method.getParameters().isEmpty()
          && method.getReturnType().getKind() != TypeKind.VOID) {
        result.add(method);
      } else {
        codegenUtil.error(
            "Facet methods must be abstract have a return a value and accept zero parameters",
            method);
      }
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * Returns the datatype of a facet element. For fields ({@link VariableElement}), returns the
   * field type. For methods ({@link ExecutableElement}), returns the method return type.
   */
  static TypeMirror getFacetElementType(Element element) {
    if (element instanceof ExecutableElement method) {
      return method.getReturnType();
    }
    return element.asType();
  }

  private Optional<DefaultFacetModel> toGivenFacetModel(
      Element facetElement,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId,
      VajramInfoLite vajramInfoLite) {
    DefaultFacetModelBuilder facetBuilder = DefaultFacetModel.builder().facetElement(facetElement);
    String facetName = facetElement.getSimpleName().toString();
    facetBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    facetBuilder.name(facetName);
    facetBuilder.documentation(elementUtils.getDocComment(facetElement));
    TypeMirror facetElementType = getFacetElementType(facetElement);
    if (TypeKind.ERROR.equals(facetElementType.getKind())) {
      throw new CodeGenShortCircuitException(
          "Vajram Id : "
              + vajramInfoLite.vajramId()
              + " facet : "
              + facetName
              + " has an error type: "
              + facetElementType);
    }

    CodeGenType dataType =
        facetElementType.accept(
            new DeclaredTypeVisitor(codegenUtil, facetElement, DISALLOWED_FACET_TYPES), null);
    facetBuilder.dataType(dataType);
    FacetType facetType = getFacetType(facetElement);
    if (facetType == null) {
      return Optional.empty();
    }
    DefaultFacetModel facetModel =
        facetBuilder.facetType(facetType).vajramInfo(vajramInfoLite).build();
    givenIdsByName.putIfAbsent(facetName, facetModel.id());
    return Optional.of(facetModel);
  }

  private @Nullable FacetType getFacetType(Element facetElement) {
    String facetName = facetElement.getSimpleName().toString();
    FacetType facetType = null;
    boolean isInput = "_Inputs".contentEquals(facetElement.getEnclosingElement().getSimpleName());
    if (isInput) {
      facetType = INPUT;
    }
    if (facetElement.getAnnotation(Inject.class) != null) {
      if (isInput) {
        codegenUtil()
            .error("Inject facet '%s' cannot be an input facet".formatted(facetName), facetElement);
      }
      facetType = INJECTION;
    }
    return facetType;
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
      Element depField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId) {
    VajramID vajramId = vajramInfo.vajramId();
    String facetName = depField.getSimpleName().toString();
    Dependency dependency = depField.getAnnotation(Dependency.class);
    DependencyModelBuilder depBuilder = DependencyModel.builder().facetElement(depField);
    depBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    depBuilder.name(facetName);
    Optional<TypeElement> vajramReqType =
        Optional.of(codegenUtil.getTypeElemFromAnnotationMember(dependency::withVajramReq))
            .filter(
                typeMirror ->
                    !typeMirror
                        .getQualifiedName()
                        .equals(
                            CodeGenUtility.getTypeElement(Request.class.getName(), processingEnv)
                                .getQualifiedName()));
    Optional<TypeElement> vajramType =
        Optional.of(codegenUtil.getTypeElemFromAnnotationMember(dependency::onVajram))
            .filter(
                typeMirror ->
                    !typeMirror
                        .getQualifiedName()
                        .equals(
                            CodeGenUtility.getTypeElement(VajramDef.class.getName(), processingEnv)
                                .getQualifiedName()));
    TypeElement vajramOrReqElement =
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
      CodeGenType declaredFieldDataType =
          getFacetElementType(depField)
              .accept(new DeclaredTypeVisitor(codegenUtil, depField, DISALLOWED_FACET_TYPES), null);
      VajramInfoLite depVajramInfoLite =
          computeVajramInfoLiteWithUpperBoundTypeArgs(vajramOrReqElement);
      depBuilder
          .depVajramInfo(depVajramInfoLite)
          .depReqType(getVajramReqTypeName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!processingEnv
          .getTypeUtils()
          .isSubtype(
              depVajramInfoLite.responseType().typeMirror(processingEnv),
              declaredFieldDataType.typeMirror(processingEnv))) {
        codegenUtil()
            .error(
                "Declared dependency facet type %s must be same as, or a super type of dependency response type %s"
                    .formatted(declaredFieldDataType, depVajramInfoLite.responseType()),
                depField);
      }
      DependencyModel depModel =
          depBuilder.dataType(declaredFieldDataType).vajramInfo(vajramInfo).build();
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

  private VajramInfoLite computeVajramInfoLiteWithExactTypeArgs(TypeMirror vajramType) {
    if (vajramType instanceof DeclaredType declaredType
        && declaredType.asElement() instanceof TypeElement typeElement) {
      return computeVajramInfoLite(typeElement, declaredType.getTypeArguments());
    } else {
      throw new AssertionError();
    }
  }

  public VajramInfoLite computeVajramInfoLiteWithUpperBoundTypeArgs(TypeElement vajramOrReqClass) {
    return computeVajramInfoLite(
        vajramOrReqClass,
        vajramOrReqClass.getTypeParameters().stream()
            .map(TypeParameterElement::asType)
            .map(TypeVariable.class::cast)
            .map(TypeVariable::getUpperBound)
            .toList());
  }

  public VajramInfoLite computeVajramInfoLite(
      TypeElement vajramOrReqClass, List<? extends TypeMirror> typeArguments) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    VajramID vajramId;
    CodeGenType responseType;
    String packageName = elementUtils.getPackageOf(vajramOrReqClass).getQualifiedName().toString();
    if (codegenUtil().isRawAssignable(vajramOrReqClass.asType(), Request.class)) {
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
      TypeMirror responseTypeMirror =
          getVajramResponseType(
              vajramOrReqClass, vajram != null ? VajramDef.class : TraitDef.class);
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
        vajramOrReqClass,
        Collections.unmodifiableList(typeArguments),
        codegenUtil.processingEnv().getElementUtils().getDocComment(vajramOrReqClass),
        this);
  }

  private ImmutableMap<String, FacetDetail> facetDetailsFromRequestType(
      TypeElement requestElement) {
    return ElementFilter.fieldsIn(requestElement.getEnclosedElements()).stream()
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
                  facetIdNameMapping.name(),
                  facetSpecField
                      .asType()
                      .accept(
                          new DeclaredTypeVisitor(codegenUtil, requestElement, ImmutableMap.of()),
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
    Optional<TypeMirror> conformsToTrait = getConformsToTraitType(vajramClass);
    VajramInfoLite conformsToTraitInfo = null;
    if (conformsToTrait.isPresent()) {
      conformsToTraitInfo = computeVajramInfoLiteWithExactTypeArgs(conformsToTrait.get());
    }
    return conformsToTraitInfo;
  }

  private Optional<TypeMirror> getConformsToTraitType(TypeElement vajramOrReqClass) {
    for (TypeMirror superInterface : vajramOrReqClass.getInterfaces()) {
      Element element = typeUtils.asElement(superInterface);
      if (element instanceof TypeElement
          && checkNotNull(element).getAnnotation(Trait.class) != null) {
        return Optional.of(superInterface);
      }
    }
    return Optional.empty();
  }

  private TypeName getVajramReqTypeName(TypeElement vajramClass) {
    ClassName className;
    if (codegenUtil().isRawAssignable(vajramClass.asType(), VajramDefRoot.class)) {
      className =
          ClassName.get(
              elementUtils.getPackageOf(vajramClass).getQualifiedName().toString(),
              vajramClass.getSimpleName() + REQUEST_SUFFIX);
    } else if (codegenUtil().isRawAssignable(vajramClass.asType(), Request.class)) {
      className = ClassName.get(vajramClass);
    } else {
      throw new AssertionError("This should not happen! Found:" + vajramClass);
    }
    return asTypeNameWithTypes(
        className,
        vajramClass.getTypeParameters().stream()
            .map(TypeParameterElement::asType)
            .map(TypeVariable.class::cast)
            .map(TypeVariable::getUpperBound)
            .toList());
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
          "Incorrect number of parameter types on Vajram interface %s. Expected 1, Found %s. Unable to infer response type for Vajram %s"
              .formatted(targetClass, typeParameters, vajramOrReqType),
          vajramOrReqType);
    }
  }

  public static String getRequestInterfaceName(String vajramName) {
    return vajramName + REQUEST_SUFFIX;
  }

  public static ClassName getRequestInterfaceName(ClassName vajramName) {
    return ClassName.get(vajramName.packageName(), vajramName.simpleName() + REQUEST_SUFFIX);
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
    return vajramName + FACETS_IMMUT_POJO_CLASS_SUFFIX;
  }

  TypeName getVajramResponseType(DependencyModel dep) {
    return getVajramResponseType(
        new TypeAndName(dep.depReqType()), codegenUtil().getTypeName(dep.dataType()));
  }

  private TypeName getVajramResponseType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(One2OneDepResponse.class),
        requestType.typeName(),
        codegenUtil().box(facetType).typeName());
  }

  public TypeName responsesType(DependencyModel dep) {
    return responsesType(
        new TypeAndName(dep.depReqType()), codegenUtil().getTypeName(dep.dataType()));
  }

  private TypeName responsesType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(FanoutDepResponses.class),
        requestType.typeName(),
        codegenUtil().box(facetType).typeName());
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
    IfAbsent ifAbsent = facet.facetElement().getAnnotation(IfAbsent.class);
    return ifAbsent != null && ifAbsent.value().usePlatformDefault();
  }

  public boolean isMandatoryOnServer(FacetGenModel facet) {
    IfAbsent ifAbsent = facet.facetElement().getAnnotation(IfAbsent.class);
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
