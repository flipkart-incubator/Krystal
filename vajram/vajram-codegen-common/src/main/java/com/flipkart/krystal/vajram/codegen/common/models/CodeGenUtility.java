package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INJECTION;
import static com.flipkart.krystal.facets.FacetType.INPUT;
import static com.flipkart.krystal.vajram.utils.Constants.IMMUT_FACETS_CLASS_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.Creator;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramDefRoot;
import com.flipkart.krystal.vajram.annos.Generated;
import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.DataTypeRegistry;
import com.flipkart.krystal.vajram.codegen.common.models.DefaultFacetModel.DefaultFacetModelBuilder;
import com.flipkart.krystal.vajram.codegen.common.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Actual;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Boxed;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.FanoutResponses;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.One2OneResponse;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.OptionalType;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassWithTooManyMethods")
@Slf4j
public class CodeGenUtility {

  private static final boolean DEBUG = false;

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
  private final Class<?> generator;
  @Getter private final DataTypeRegistry dataTypeRegistry;

  public CodeGenUtility(ProcessingEnvironment processingEnv, Class<?> generator) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.generator = generator;
    this.dataTypeRegistry = new DataTypeRegistry();
  }

  public static ClassName toClassName(String depReqClassName) {
    int lastDotIndex = depReqClassName.lastIndexOf('.');
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  public static List<AnnotationSpec> recordAnnotations() {
    return List.of(
        AnnotationSpec.builder(EqualsAndHashCode.class).build(),
        AnnotationSpec.builder(ToString.class).addMember("doNotUseGetters", "true").build());
  }

  public static List<AnnotationSpec> annotations(Class<?>... annotations) {
    return stream(annotations).map(aClass -> AnnotationSpec.builder(aClass).build()).toList();
  }

  public IfAbsent getIfAbsent(Element element) {
    // Check if the element has the @IfAbsent annotation
    IfAbsent ifAbsent = element.getAnnotation(IfAbsent.class);
    if (ifAbsent == null) {
      ifAbsent = Creator.create(IfAbsentThen.WILL_NEVER_FAIL, "");
    }
    return ifAbsent;
  }

  /**
   * Extracts and validates model methods from the model root interface.
   *
   * @param modelRootType The type element representing the model root
   * @return List of validated executable elements representing model methods
   */
  public List<ExecutableElement> extractAndValidateModelMethods(TypeElement modelRootType) {
    List<ExecutableElement> modelMethods = new ArrayList<>();

    for (ExecutableElement executableElem :
        ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(modelRootType))) {
      if (ElementKind.METHOD.equals(executableElem.getKind())
          && executableElem.getModifiers().contains(ABSTRACT)) {
        if (executableElem.getSimpleName().toString().startsWith("_")) {
          // Methods whose names start with an '_' are considered "meta" methods which are not
          // used to access actual model data. So they are ignored.
          continue;
        }
        validateGetterMethod(executableElem);

        modelMethods.add(executableElem);
      }
    }

    return modelMethods;
  }

  private void validateGetterMethod(ExecutableElement method) {
    // Validate method has zero parameters
    if (!method.getParameters().isEmpty()) {
      error("Model root methods must have zero parameters: " + method.getSimpleName(), method);
    }

    TypeMirror returnType = method.getReturnType();

    // Validate method has a return type (not void)
    if (returnType.getKind() == TypeKind.VOID) {
      error(
          "Model root methods must have a return type (not void): " + method.getSimpleName(),
          method);
    }

    // Validate method return type is not an array
    if (returnType.getKind() == TypeKind.ARRAY) {
      error("Model root methods must not return arrays. Use List instead.", method);
    }
  }

  public boolean isNullable(TypeMirror typeMirror) {
    return typeMirror.getAnnotationMirrors().stream()
        .map(AnnotationMirror::getAnnotationType)
        .map(DeclaredType::asElement)
        .filter(type -> type instanceof QualifiedNameable)
        .map(type -> (QualifiedNameable) type)
        .anyMatch(
            element -> element.getQualifiedName().contentEquals(Nullable.class.getCanonicalName()));
  }

  public boolean isOptional(TypeMirror returnType) {
    return isRawAssignable(returnType, Optional.class);
  }

  public TypeMirror getOptionalInnerType(TypeMirror optionalType) {
    if (!isOptional(optionalType)) {
      return optionalType;
    }

    if (optionalType instanceof DeclaredType declaredType) {
      if (!declaredType.getTypeArguments().isEmpty()) {
        return declaredType.getTypeArguments().get(0);
      }
    }

    return requireNonNull(
            processingEnv().getElementUtils().getTypeElement(Optional.class.getCanonicalName()))
        .asType();
  }

  @Nullable String getDisallowedMessage(
      TypeMirror type, ImmutableMap<Class<?>, String> disallowedTypes) {
    return disallowedTypes.entrySet().stream()
        .<@Nullable String>map(
            e -> {
              if (isRawAssignable(type, e.getKey(), processingEnv())) {
                return e.getValue();
              } else {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns true if the raw type (without generics) of {@code from} can be assigned to the raw type
   * of {@code to}
   */
  public static boolean isRawAssignable(
      TypeMirror from, Class<?> to, ProcessingEnvironment processingEnv) {
    Types typeUtils = processingEnv.getTypeUtils();
    return typeUtils.isAssignable(
        typeUtils.erasure(from),
        typeUtils.erasure(
            getTypeElement(checkNotNull(to.getCanonicalName()), processingEnv).asType()));
  }

  public static TypeElement getTypeElement(String name, ProcessingEnvironment processingEnv) {
    TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(name);
    if (typeElement == null) {
      throw new IllegalStateException("Could not find type element with name %s".formatted(name));
    }
    return typeElement;
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
        IfAbsent ifAbsent = getIfAbsent(facet.facetField());
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
      throw errorAndThrow(
          "Qualified Facet is not of the form <vajramId>:<facetName> : " + qualifiedFacet,
          resolverMethod);
    }
    if (!vajramId.equals(parts.get(0))) {
      error(
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

  public void generateSourceFile(String className, String code, TypeElement vajramDefinition) {
    try {
      JavaFileObject requestFile =
          processingEnv.getFiler().createSourceFile(className, vajramDefinition);
      note("Successfully Create source file %s".formatted(className));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.println(code);
      }
    } catch (Exception e) {
      error(
          "Error creating java file for className: %s. Error: %s".formatted(className, e),
          vajramDefinition);
    }
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
    List<VariableElement> injectionFields =
        internalFacetFields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Inject.class) != null)
            .toList();
    AtomicInteger nextFacetId = new AtomicInteger(1);
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramInfoLite,
            Streams.concat(
                    inputFields.stream()
                        .map(
                            inputField ->
                                toGivenFacetModel(
                                    inputField,
                                    true,
                                    givenIdsByName,
                                    takenFacetIds,
                                    nextFacetId,
                                    vajramInfoLite)),
                    injectionFields.stream()
                        .map(
                            internalField ->
                                toGivenFacetModel(
                                    internalField,
                                    false,
                                    givenIdsByName,
                                    takenFacetIds,
                                    nextFacetId,
                                    vajramInfoLite))
                        .filter(facet -> facet.facetType().equals(INJECTION)))
                .collect(toImmutableList()),
            dependencyFields.stream()
                .map(
                    depField ->
                        Optional.ofNullable(
                            toDependencyModel(
                                vajramInfoLite.vajramId(),
                                depField,
                                givenIdsByName,
                                takenFacetIds,
                                nextFacetId)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList()),
            conformsToTraitInfo);
    note("VajramInfo: %s".formatted(vajramInfo));
    validateVajramInfo(vajramInfo);
    return vajramInfo;
  }

  private void validateVajramInfo(VajramInfo vajramInfo) {
    vajramInfo
        .facetStream()
        .forEach(
            facetGenModel -> {
              if (facetGenModel.name().startsWith("_")) {
                error(
                    "Facet names cannot start with an underscore (_). These are reserved for platform specific identifiers",
                    facetGenModel.facetField());
              }
            });
  }

  private DefaultFacetModel toGivenFacetModel(
      VariableElement facetField,
      boolean isInput,
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
    CodeGenType dataType =
        facetField
            .asType()
            .accept(new DeclaredTypeVisitor(this, facetField, DISALLOWED_FACET_TYPES), null);
    facetBuilder.dataType(dataType);
    FacetType facetType = null;
    if (isInput) {
      facetType = INPUT;
    }
    if (facetField.getAnnotation(Inject.class) != null) {
      if (isInput) {
        error("Inject facet '%s' cannot be an input facet".formatted(facetName), facetField);
      }
      facetType = INJECTION;
    }
    if (facetType == null) {
      throw errorAndThrow(
          "Facet '%s' is not an input facet or an injection facet".formatted(facetName),
          facetField);
    }
    DefaultFacetModel facetModel =
        facetBuilder.facetType(requireNonNull(facetType)).vajramInfo(vajramInfoLite).build();
    givenIdsByName.putIfAbsent(facetName, facetModel.id());
    return facetModel;
  }

  private static int getNextAvailableFacetId(
      Set<Integer> takenFacetIds, AtomicInteger nextFacetId) {
    while (takenFacetIds.contains(nextFacetId.get())) {
      nextFacetId.getAndIncrement();
    }
    return nextFacetId.getAndIncrement();
  }

  private @Nullable DependencyModel toDependencyModel(
      VajramID vajramId,
      VariableElement depField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId) {
    String facetName = depField.getSimpleName().toString();
    Dependency dependency = depField.getAnnotation(Dependency.class);
    DependencyModelBuilder depBuilder = DependencyModel.builder().facetField(depField);
    depBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    depBuilder.name(facetName);
    Optional<TypeMirror> vajramReqType =
        getTypeFromAnnotationMember(dependency::withVajramReq)
            .filter(
                typeMirror ->
                    !checkNotNull((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            getTypeElement(Request.class.getName(), processingEnv)
                                .getQualifiedName()));
    Optional<TypeMirror> vajramType =
        getTypeFromAnnotationMember(dependency::onVajram)
            .filter(
                typeMirror ->
                    !checkNotNull((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            getTypeElement(VajramDef.class.getName(), processingEnv)
                                .getQualifiedName()));
    TypeMirror vajramOrReqType =
        vajramReqType
            .or(() -> vajramType)
            .orElseThrow(
                () -> {
                  error(
                      "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                          .formatted(depField.getSimpleName(), vajramId),
                      depField);
                  return new VajramDefinitionException("Invalid Dependency specification");
                });
    depBuilder.documentation(elementUtils.getDocComment(depField));
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      error(
          ("Both `withVajramReq` and `onVajram` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'."
                  + " Found withVajramReq=%s and onVajram=%s")
              .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
          depField);
    } else {
      CodeGenType declaredDataType =
          new DeclaredTypeVisitor(this, depField, DISALLOWED_FACET_TYPES).visit(depField.asType());
      TypeElement vajramOrReqElement =
          checkNotNull((TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType));
      VajramInfoLite depVajramInfoLite = computeVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramInfo(depVajramInfoLite)
          .depReqClassQualifiedName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!declaredDataType.equals(depVajramInfoLite.responseType())) {
        error(
            "Declared dependency type %s does not match dependency vajram response type %s"
                .formatted(declaredDataType, depVajramInfoLite.responseType()),
            depField);
      }
      DependencyModel depModel =
          depBuilder.dataType(declaredDataType).vajramInfo(depVajramInfoLite).build();
      givenIdsByName.putIfAbsent(facetName, depModel.id());
      return depModel;
    }
    error(
        ("Invalid dependency spec of dependency '%s' of vajram '%s'."
                + " Found withVajramReq=%s and onVajram=%s")
            .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
        depField);
    return null;
  }

  private VajramInfoLite computeVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    ImmutableBiMap<Integer, String> facetIdNameMappings = ImmutableBiMap.of();
    VajramID vajramId;
    CodeGenType responseType;
    String packageName = elementUtils.getPackageOf(vajramOrReqClass).getQualifiedName().toString();
    if (isRawAssignable(vajramOrReqClass.asType(), Request.class)) {
      facetIdNameMappings =
          ElementFilter.fieldsIn(vajramOrReqClass.getEnclosedElements()).stream()
              .filter(
                  element ->
                      element.asType() instanceof DeclaredType d
                          && d.asElement() instanceof TypeElement t
                          && t.getQualifiedName()
                              .contentEquals(InputMirrorSpec.class.getCanonicalName()))
              .map(element -> element.getAnnotation(FacetIdNameMapping.class))
              .filter(Objects::nonNull)
              .collect(toImmutableBiMap(FacetIdNameMapping::id, FacetIdNameMapping::name));
      TypeMirror responseTypeMirror = getVajramResponseType(vajramOrReqClass, Request.class);
      TypeElement responseTypeElement =
          checkNotNull((TypeElement) typeUtils.asElement(responseTypeMirror));
      vajramId =
          vajramID(
              vajramClassSimpleName.substring(
                  0, vajramClassSimpleName.length() - Constants.REQUEST_SUFFIX.length()));
      responseType =
          new DeclaredTypeVisitor(this, responseTypeElement, DISALLOWED_FACET_TYPES)
              .visit(responseTypeMirror);
    } else if (isRawAssignable(vajramOrReqClass.asType(), VajramDefRoot.class)) {
      Vajram vajram = vajramOrReqClass.getAnnotation(Vajram.class);
      Trait trait = vajramOrReqClass.getAnnotation(Trait.class);
      if (vajram == null && trait == null) {
        throw new VajramValidationException(
            "Vajram class %s does not have either @VajramDef or @VajramTrait annotation. This should not happen"
                .formatted(vajramOrReqClass));
      }
      TypeMirror responseTypeMirror = getVajramResponseType(vajramOrReqClass, VajramDefRoot.class);
      TypeElement responseTypeElement =
          checkNotNull((TypeElement) typeUtils.asElement(responseTypeMirror));
      TypeElement requestType =
          elementUtils.getTypeElement(
              packageName + "." + getRequestInterfaceName(vajramClassSimpleName));
      if (requestType != null) {
        facetIdNameMappings =
            ElementFilter.fieldsIn(requestType.getEnclosedElements()).stream()
                .filter(
                    element ->
                        element.asType() instanceof DeclaredType d
                            && d.asElement() instanceof TypeElement t
                            && t.getQualifiedName()
                                .contentEquals(InputMirrorSpec.class.getCanonicalName()))
                .map(element -> element.getAnnotation(FacetIdNameMapping.class))
                .filter(Objects::nonNull)
                .collect(toImmutableBiMap(FacetIdNameMapping::id, FacetIdNameMapping::name));
      }
      vajramId = vajramID(vajramClassSimpleName);
      responseType =
          new DeclaredTypeVisitor(this, responseTypeElement, DISALLOWED_FACET_TYPES)
              .visit(responseTypeMirror);
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, VajramDef.class, ImmutableRequest.class));
    }
    return new VajramInfoLite(
        vajramId, responseType, packageName, facetIdNameMappings, vajramOrReqClass, this);
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

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (isRawAssignable(vajramClass.asType(), VajramDefRoot.class)) {
      return vajramClass.getQualifiedName().toString() + Constants.REQUEST_SUFFIX;
    } else if (isRawAssignable(vajramClass.asType(), Request.class)) {
      return vajramClass.getQualifiedName().toString();
    } else {
      throw new AssertionError("This should not happen! Found:" + vajramClass);
    }
  }

  public Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> supplier) {
    try {
      var ignored = supplier.get();
      throw new AssertionError("Expected supplier to throw error");
    } catch (MirroredTypeException mte) {
      return Optional.ofNullable(mte.getTypeMirror());
    }
  }

  public List<? extends TypeMirror> getTypesFromAnnotationMember(Supplier<Class<?>[]> supplier) {
    try {
      var ignored = supplier.get();
      return List.of();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors();
    }
  }

  private TypeMirror getVajramResponseType(TypeElement vajramOrReqType, Class<?> targetClass) {
    int typeParamIndex = 0;
    List<? extends TypeMirror> typeParameters =
        getTypeParamTypes(
            vajramOrReqType,
            requireNonNull(
                elementUtils.getTypeElement(requireNonNull(targetClass.getCanonicalName()))));
    if (typeParameters.size() > typeParamIndex) {
      return typeParameters.get(typeParamIndex);
    } else {
      throw errorAndThrow(
          "Incorrect number of parameter types on Vajram interface. Expected 1, Found %s. Unable to infer response type for Vajram %s"
              .formatted(typeParameters, vajramOrReqType.getQualifiedName()),
          vajramOrReqType);
    }
  }

  public ImmutableList<TypeMirror> getTypeParamTypes(
      TypeElement childTypeElement, TypeElement targetParentClass) {
    List<TypeMirror> currentTypes = List.of(childTypeElement.asType());
    note("VajramDef: %s".formatted(childTypeElement));

    Types typeUtils = processingEnv.getTypeUtils();
    DeclaredType targetType = null;
    do {
      List<TypeMirror> newSuperTypes = new ArrayList<>();
      for (TypeMirror currentType : currentTypes) {
        List<DeclaredType> superTypes =
            processingEnv.getTypeUtils().directSupertypes(currentType).stream()
                .filter(t -> (t instanceof DeclaredType))
                .map(t -> (DeclaredType) t)
                .toList();
        newSuperTypes.addAll(superTypes);
        for (DeclaredType superType : superTypes) {
          note("SuperType: %s [%s]".formatted(superType, superType.getClass()));
          Element element = typeUtils.asElement(superType);
          if (element instanceof TypeElement typeElement) {
            note("Element qualified name: %s".formatted(typeElement.getQualifiedName()));
            if (typeElement
                .getQualifiedName()
                .contentEquals(targetParentClass.getQualifiedName())) {
              targetType = superType;
              break;
            }
          }
        }
        note("CurrentElement: %s".formatted(currentType));
      }
      if (targetType == null) {
        currentTypes = newSuperTypes;
      }
    } while (!currentTypes.isEmpty() && targetType == null);
    if (targetType != null) {
      return ImmutableList.copyOf(getTypeMirrors(targetType));
    }
    return ImmutableList.of();
  }

  private static List<? extends TypeMirror> getTypeMirrors(DeclaredType targetType) {
    List<? extends TypeMirror> typeParameters = targetType.getTypeArguments();
    return typeParameters;
  }

  public void note(CharSequence message) {
    if (DEBUG) {
      processingEnv
          .getMessager()
          .printMessage(Kind.NOTE, "[%s] %s".formatted(getTimestamp(), message));
    }
  }

  public CodeValidationException errorAndThrow(String message, @Nullable Element... elements) {
    error(message, elements);
    return new CodeValidationException(message);
  }

  public void error(String message, @Nullable Element... elements) {
    if (elements.length == 0) {
      processingEnv.getMessager().printMessage(Kind.ERROR, message);
    } else {
      for (Element element : elements) {
        processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
      }
    }
  }

  private String getTimestamp() {
    return ISO_OFFSET_DATE_TIME.format(
        OffsetDateTime.now(ZoneId.of(checkNotNull(ZoneId.SHORT_IDS.get("IST")))));
  }

  public static String getRequestInterfaceName(String vajramName) {
    return vajramName + Constants.REQUEST_SUFFIX;
  }

  public static String getImmutRequestInterfaceName(String vajramName) {
    return vajramName + Constants.IMMUT_REQUEST_SUFFIX;
  }

  public static String getImmutRequestPojoName(String vajramName) {
    return vajramName + Constants.IMMUT_REQUEST_POJO_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + Constants.IMPL_SUFFIX;
  }

  public static String getFacetsInterfaceName(String vajramName) {
    return vajramName + Constants.FACETS_CLASS_SUFFIX;
  }

  public static String getImmutFacetsClassName(String vajramName) {
    return vajramName + IMMUT_FACETS_CLASS_SUFFIX;
  }

  public TypeName toTypeName(CodeGenType dataType) {
    return TypeName.get(dataType.javaModelType(processingEnv));
  }

  public static TypeName toTypeName(Type typeArg) {
    if (typeArg instanceof ParameterizedType parameterizedType) {
      final Type rawType = parameterizedType.getRawType();
      final Type[] typeArgs = parameterizedType.getActualTypeArguments();
      return ParameterizedTypeName.get(
          (ClassName) toTypeName(rawType),
          stream(typeArgs).map(CodeGenUtility::toTypeName).toArray(TypeName[]::new));
    } else {
      if (typeArg instanceof Class<?>) {
        return ClassName.get(Primitives.wrap((Class<?>) typeArg));
      } else {
        return ClassName.bestGuess(typeArg.getTypeName());
      }
    }
  }

  public static List<? extends TypeMirror> getTypeParameters(TypeMirror returnType) {
    return returnType.accept(
        new SimpleTypeVisitor14<List<? extends TypeMirror>, Void>() {
          @Override
          public List<? extends TypeMirror> visitDeclared(DeclaredType t, Void unused) {
            return t.getTypeArguments();
          }
        },
        null);
  }

  public boolean isSameRawType(TypeMirror a, Class<?> b) {
    return processingEnv
        .getTypeUtils()
        .isSameType(
            typeUtils.erasure(a),
            typeUtils.erasure(
                checkNotNull(
                        processingEnv()
                            .getElementUtils()
                            .getTypeElement(checkNotNull(b.getCanonicalName())),
                        "TypeElement not found for: " + b.getCanonicalName())
                    .asType()));
  }

  /**
   * Returns true if the raw type (without generics) of {@code from} can be assigned to the raw type
   * of {@code to}
   */
  public boolean isRawAssignable(TypeMirror from, Class<?> to) {
    return isRawAssignable(from, to, processingEnv());
  }

  public TypeMirror box(TypeMirror type) {
    if (type instanceof PrimitiveType p) {
      return typeUtils.boxedClass(p).asType();
    } else {
      return type;
    }
  }

  /**
   * Creates a class builder with the given class name. If the className is a blank string, then the
   * builder represents an anonymous class.
   *
   * @param className fully qualified class name
   * @param generatedForCanonicalName canonical name of the originating class for which the class is
   *     being generated
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder classBuilder(String className, String generatedForCanonicalName) {
    TypeSpec.Builder classBuilder;
    if (className.isBlank()) {
      classBuilder = TypeSpec.anonymousClassBuilder("");
    } else {
      classBuilder = TypeSpec.classBuilder(className);
    }
    classBuilder.addJavadoc("@see $L", generatedForCanonicalName);
    addDefaultAnnotations(classBuilder);
    return classBuilder;
  }

  /**
   * Creates a class builder with the given class name. If the interfaceName is a blank string, then
   * the builder represents an anonymous class.
   *
   * @param interfaceName fully qualified class name
   * @param generatedForCanonicalName canonical name of the originating class for which the
   *     interface is being generated
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder interfaceBuilder(String interfaceName, String generatedForCanonicalName) {
    TypeSpec.Builder interfaceBuilder;
    if (interfaceName.isBlank()) {
      throw new RuntimeException("interface name cannot be blank");
    } else {
      interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName);
    }
    addDefaultAnnotations(interfaceBuilder);
    interfaceBuilder.addJavadoc("@see $L", generatedForCanonicalName);
    return interfaceBuilder;
  }

  private void addDefaultAnnotations(TypeSpec.Builder classBuilder) {
    classBuilder.addAnnotation(
        AnnotationSpec.builder(SuppressWarnings.class)
            .addMember(
                "value",
                Stream.of(
                        CodeBlock.of("$S", "unchecked"),
                        CodeBlock.of("$S", "ClassReferencesSubclass"))
                    .collect(joining(",", "{", "}")))
            .build());
    addGeneratedAnnotations(classBuilder);
  }

  public void addGeneratedAnnotations(TypeSpec.Builder classBuilder) {
    classBuilder
        .addAnnotation(
            AnnotationSpec.builder(Generated.class)
                .addMember("by", "$S", generator.getName())
                .build())
        .addAnnotation(
            AnnotationSpec.builder(javax.annotation.processing.Generated.class)
                .addMember("value", "$S", generator.getName())
                .addMember("date", "$S", getTimestamp())
                .build());
  }

  public TypeAndName box(TypeAndName javaType) {
    @Nullable TypeMirror typeMirror = javaType.type();
    if (typeMirror == null) {
      return javaType;
    }
    TypeKind typeKind = typeMirror.getKind();
    if (!typeKind.isPrimitive() && typeKind != TypeKind.VOID) {
      return javaType;
    }
    TypeMirror boxed;
    if (typeKind == TypeKind.VOID) {
      boxed =
          requireNonNull(
                  processingEnv.getElementUtils().getTypeElement(Void.class.getCanonicalName()))
              .asType();
    } else {
      boxed = processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType();
    }
    return new TypeAndName(
        TypeName.get(boxed).annotated(javaType.annotationSpecs()),
        boxed,
        javaType.annotationSpecs());
  }

  TypeName optional(TypeAndName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), box(javaType).typeName());
  }

  TypeName responseType(DependencyModel dep) {
    return responseType(
        new TypeAndName(toClassName(dep.depReqClassQualifiedName())), getTypeName(dep.dataType()));
  }

  private TypeName responseType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(One2OneDepResponse.class), requestType.typeName(), box(facetType).typeName());
  }

  public TypeName responsesType(DependencyModel dep) {
    return responsesType(
        new TypeAndName(toClassName(dep.depReqClassQualifiedName())), getTypeName(dep.dataType()));
  }

  private TypeName responsesType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(FanoutDepResponses.class), requestType.typeName(), box(facetType).typeName());
  }

  TypeAndName getTypeName(CodeGenType dataType, List<AnnotationSpec> typeAnnotations) {
    TypeMirror javaModelType = dataType.javaModelType(processingEnv);
    return new TypeAndName(
        TypeName.get(javaModelType).annotated(typeAnnotations), javaModelType, typeAnnotations);
  }

  public TypeAndName getTypeName(CodeGenType dataType) {
    return getTypeName(dataType, List.of());
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

  @SuppressWarnings("method.invocation")
  public ExecutableElement getMethod(Class<?> clazz, String methodName, int paramCount) {
    return requireNonNull(
            processingEnv()
                .getElementUtils()
                .getTypeElement(requireNonNull(clazz.getCanonicalName())))
        .getEnclosedElements()
        .stream()
        .filter(element -> element instanceof ExecutableElement)
        .map(element -> (ExecutableElement) element)
        .filter(
            element ->
                element.getSimpleName().contentEquals(methodName)
                    && element.getParameters().size() == paramCount)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Could not find method '"
                        + methodName
                        + "' with param count '"
                        + paramCount
                        + "' in class "
                        + clazz));
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

  public String getJavaTypeCreationCode(CodeGenType javaType, List<TypeName> collectClassNames) {
    TypeMirror typeMirror = javaType.javaModelType(processingEnv);
    collectClassNames.add(ClassName.get(JavaType.class));
    if (javaType.typeParameters().isEmpty()) {
      collectClassNames.add(TypeName.get(typeMirror));
      return "$T.create($T.class)";
    } else {
      collectClassNames.add(TypeName.get(processingEnv.getTypeUtils().erasure(typeMirror)));
      return "$T.create($T.class, "
          + javaType.typeParameters().stream()
              .map(dataType -> getJavaTypeCreationCode(dataType, collectClassNames))
              .collect(Collectors.joining(","))
          + ")";
    }
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
      throw errorAndThrow(
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
