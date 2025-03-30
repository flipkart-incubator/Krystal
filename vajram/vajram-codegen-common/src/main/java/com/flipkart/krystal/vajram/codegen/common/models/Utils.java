package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.vajram.utils.Constants.IMMUT_FACETS_CLASS_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.annos.ConformsToTrait;
import com.flipkart.krystal.vajram.annos.Generated;
import com.flipkart.krystal.vajram.codegen.common.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Actual;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.Boxed;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.FanoutResponses;
import com.flipkart.krystal.vajram.codegen.common.models.FacetJavaType.One2OneResponse;
import com.flipkart.krystal.vajram.codegen.common.models.GivenFacetModel.GivenFacetModelBuilder;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FacetId;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.ReservedFacets;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassWithTooManyMethods")
@Slf4j
public class Utils {

  private static final boolean DEBUG = false;
  private static final ImmutableMap<Class<?>, String> DISALLOWED_SUPERTYPES_AT_TOP_LEVEL =
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
      Splitter.onPattern(Constants.QUALIFIED_FACET_SEPERATOR);

  @Getter private final ProcessingEnvironment processingEnv;
  private final Types typeUtils;
  private final Elements elementUtils;
  private final Class<?> generator;

  public Utils(ProcessingEnvironment processingEnv, Class<?> generator) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.generator = generator;
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

  static @Nullable String getDisallowedMessage(
      TypeMirror type, ProcessingEnvironment processingEnv) {
    return DISALLOWED_SUPERTYPES_AT_TOP_LEVEL.entrySet().stream()
        .<@Nullable String>map(
            e -> {
              if (isRawAssignable(type, e.getKey(), processingEnv)) {
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

  public String extractFacetName(
      String vajramId, String qualifiedFacet, ExecutableElement resolverMethod) {
    List<String> parts = QUALIFIED_FACET_SPLITTER.splitToList(qualifiedFacet);
    if (parts.size() != 2) {
      throw errorAndThrow(
          "Qualified Facet is not of the form <vajramId>:<facetName> : " + qualifiedFacet,
          resolverMethod);
    }
    if (!vajramId.equals(parts.get(0))) {
      throw errorAndThrow(
          "Expected vajram id '"
              + vajramId
              + "' does not match with the given qualified facet: "
              + qualifiedFacet,
          resolverMethod);
    }
    return parts.get(1);
  }

  public List<TypeElement> getDefinitionClasses(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWithAny(Set.of(Vajram.class, Trait.class)).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
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
    VajramInfoLite vajramInfoLite = getVajramInfoLite(vajramClass);
    boolean isVajram = vajramClass.getAnnotation(Vajram.class) != null;
    Optional<Element> facetsClass =
        vajramClass.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .filter(element -> element.getSimpleName().contentEquals(Constants._FACETS_CLASS))
            .findFirst()
            .map(element -> typeUtils.asElement(element.asType()));
    Set<Integer> reservedFacets =
        facetsClass
            .map(f -> f.getAnnotation(ReservedFacets.class))
            .map(ReservedFacets::ids)
            .map(IntStream::of)
            .map(IntStream::boxed)
            .map(integerStream -> integerStream.collect(toImmutableSet()))
            .orElse(ImmutableSet.of());
    List<VariableElement> fields =
        ElementFilter.fieldsIn(facetsClass.map(Element::getEnclosedElements).orElse(List.of()));
    BiMap<String, Integer> givenIdsByName = HashBiMap.create();
    for (VariableElement field : fields) {
      Optional<Integer> i =
          Optional.ofNullable(field.getAnnotation(FacetId.class)).map(FacetId::value);
      if (i.isPresent()) {
        int givenId = i.get();
        String facetName = field.getSimpleName().toString();
        if (reservedFacets.contains(givenId)) {
          throw errorAndThrow(
              "Facet %s cannot use the reserved facet id %d".formatted(facetName, givenId), field);
        } else if (givenIdsByName.inverse().containsKey(givenId)) {
          throw errorAndThrow(
              "FacetId %d is already assigned to Facet %s"
                  .formatted(
                      givenId, givenIdsByName.inverse().getOrDefault(givenId, "unknown facet")),
              field);
        } else {
          givenIdsByName.put(facetName, givenId);
        }
      }
    }
    Set<Integer> takenFacetIds = Sets.union(reservedFacets, givenIdsByName.values());
    List<VariableElement> inputFields =
        fields.stream()
            .filter(
                variableElement ->
                    variableElement.getAnnotation(Input.class) != null
                        || (isVajram && variableElement.getAnnotation(Inject.class) != null))
            .toList();
    List<VariableElement> dependencyFields =
        isVajram
            ? fields.stream()
                .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
                .toList()
            : List.of();
    AtomicInteger nextFacetId = new AtomicInteger(1);
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramInfoLite,
            inputFields.stream()
                .map(
                    inputField ->
                        toInputModel(
                            inputField, givenIdsByName, takenFacetIds, nextFacetId, vajramInfoLite))
                .collect(toImmutableList()),
            dependencyFields.stream()
                .map(
                    depField ->
                        toDependencyModel(
                            vajramInfoLite.vajramId(),
                            depField,
                            givenIdsByName,
                            takenFacetIds,
                            nextFacetId))
                .collect(toImmutableList()));
    note("VajramInfo: %s".formatted(vajramInfo));
    return vajramInfo;
  }

  private GivenFacetModel toInputModel(
      VariableElement inputField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId,
      VajramInfoLite vajramInfoLite) {
    GivenFacetModelBuilder inputBuilder = GivenFacetModel.builder().facetField(inputField);
    String facetName = inputField.getSimpleName().toString();
    inputBuilder.id(
        requireNonNullElseGet(
            givenIdsByName.get(facetName),
            () -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    inputBuilder.name(facetName);
    inputBuilder.documentation(elementUtils.getDocComment(inputField));
    DataType<Object> dataType =
        inputField.asType().accept(new DeclaredTypeVisitor<>(this, inputField), null);
    inputBuilder.dataType(dataType);
    EnumSet<FacetType> facetTypes = EnumSet.noneOf(FacetType.class);
    if (inputField.getAnnotation(Input.class) != null) {
      facetTypes.add(FacetType.INPUT);
    }
    if (inputField.getAnnotation(Inject.class) != null) {
      facetTypes.add(FacetType.INJECTION);
    }
    GivenFacetModel givenFacetModel =
        inputBuilder.facetTypes(facetTypes).vajramInfo(vajramInfoLite).build();
    givenIdsByName.putIfAbsent(facetName, givenFacetModel.id());
    return givenFacetModel;
  }

  private static int getNextAvailableFacetId(
      Set<Integer> takenFacetIds, AtomicInteger nextFacetId) {
    while (takenFacetIds.contains(nextFacetId.get())) {
      nextFacetId.getAndIncrement();
    }
    return nextFacetId.getAndIncrement();
  }

  private DependencyModel toDependencyModel(
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
      DataType<?> declaredDataType =
          new DeclaredTypeVisitor<@NonNull Object>(this, depField).visit(depField.asType());
      TypeElement vajramOrReqElement =
          checkNotNull((TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType));
      VajramInfoLite depVajramInfoLite = getVajramInfoLite(vajramOrReqElement);
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
    throw errorAndThrow(
        ("Invalid dependency spec of dependency '%s' of vajram '%s'."
                + " Found withVajramReq=%s and onVajram=%s")
            .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
        depField);
  }

  private VajramInfoLite getVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    ImmutableBiMap<Integer, String> facetIdNameMappings = ImmutableBiMap.of();
    VajramInfoLite conformsToTraitInfo = getConformToTraitInfo(vajramOrReqClass);
    VajramID vajramId;
    DataType<Object> responseType;
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
      TypeMirror responseTypeMirror = getResponseType(vajramOrReqClass, Request.class);
      TypeElement responseTypeElement =
          checkNotNull((TypeElement) typeUtils.asElement(responseTypeMirror));
      vajramId =
          vajramID(
              vajramClassSimpleName.substring(
                  0, vajramClassSimpleName.length() - Constants.REQUEST_SUFFIX.length()));
      responseType =
          new DeclaredTypeVisitor<@NonNull Object>(this, responseTypeElement)
              .visit(responseTypeMirror);
    } else if (isRawAssignable(vajramOrReqClass.asType(), VajramDef.class)
        || isRawAssignable(vajramOrReqClass.asType(), TraitDef.class)) {
      Vajram vajram = vajramOrReqClass.getAnnotation(Vajram.class);
      Trait trait = vajramOrReqClass.getAnnotation(Trait.class);
      if (vajram == null && trait == null) {
        throw new VajramValidationException(
            "Vajram class %s does not have either @VajramDef or @VajramTrait annotation. This should not happen"
                .formatted(vajramOrReqClass));
      }
      boolean isTrait = trait != null;
      TypeMirror responseTypeMirror =
          getResponseType(vajramOrReqClass, isTrait ? TraitDef.class : VajramDef.class);
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
          new DeclaredTypeVisitor<@NonNull Object>(this, responseTypeElement)
              .visit(responseTypeMirror);
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, VajramDef.class, ImmutableRequest.class));
    }
    return new VajramInfoLite(
        vajramId,
        responseType,
        packageName,
        facetIdNameMappings,
        conformsToTraitInfo,
        vajramOrReqClass,
        this);
  }

  @SuppressWarnings("method.invocation")
  private @Nullable VajramInfoLite getConformToTraitInfo(TypeElement vajramOrReqClass) {
    ConformsToTrait conformsToTrait = vajramOrReqClass.getAnnotation(ConformsToTrait.class);
    VajramInfoLite conformsToTraitInfo = null;
    if (conformsToTrait != null) {
      Optional<TypeMirror> traitType =
          getTypeFromAnnotationMember(conformsToTrait::withDef)
              .filter(
                  typeMirror ->
                      !checkNotNull((QualifiedNameable) typeUtils.asElement(typeMirror))
                          .getQualifiedName()
                          .equals(
                              getTypeElement(TraitDef.class.getName(), processingEnv)
                                  .getQualifiedName()));

      if (traitType.isEmpty()) {
        throw new VajramValidationException(
            "Either trait or traitRequest must be set in @ConformsTo annotation of vajram %s"
                .formatted(vajramOrReqClass));
      }
      conformsToTraitInfo =
          getVajramInfoLite(
              checkNotNull(
                  (TypeElement)
                      processingEnv
                          .getTypeUtils()
                          .asElement(requireNonNull(traitType.orElse(null)))));
    }
    return conformsToTraitInfo;
  }

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (isRawAssignable(vajramClass.asType(), VajramDef.class)
        || isRawAssignable(vajramClass.asType(), TraitDef.class)) {
      return vajramClass.getQualifiedName().toString() + Constants.REQUEST_SUFFIX;
    } else if (isRawAssignable(vajramClass.asType(), Request.class)) {
      return vajramClass.getQualifiedName().toString();
    } else {
      throw new AssertionError("This should not happen! Found:" + vajramClass);
    }
  }

  Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> runnable) {
    try {
      var ignored = runnable.get();
      throw new AssertionError();
    } catch (MirroredTypeException mte) {
      return Optional.ofNullable(mte.getTypeMirror());
    }
  }

  private TypeMirror getResponseType(TypeElement vajramDef, Class<?> targetClass) {
    int typeParamIndex = 0;
    List<TypeMirror> currentTypes = List.of(vajramDef.asType());
    note("VajramDef: %s".formatted(vajramDef));

    Types typeUtils = processingEnv.getTypeUtils();
    DeclaredType vajramInterface = null;
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
            if (typeElement.getQualifiedName().contentEquals(targetClass.getName())) {
              vajramInterface = superType;
              break;
            }
          }
        }
        note("CurrentElement: %s".formatted(currentType));
      }
      if (vajramInterface == null) {
        currentTypes = newSuperTypes;
      }
    } while (!currentTypes.isEmpty() && vajramInterface == null);
    if (vajramInterface != null) {
      List<? extends TypeMirror> typeParameters = vajramInterface.getTypeArguments();
      if (typeParameters.size() > typeParamIndex) {
        return typeParameters.get(typeParamIndex);
      } else {
        error(
            "Incorrect number of parameter types on Vajram interface. Expected 1, Found %s"
                .formatted(typeParameters),
            vajramDef);
      }
    }
    error(
        "Unable to infer response type for Vajram %s".formatted(vajramDef.getQualifiedName()),
        vajramDef);
    throw new RuntimeException();
  }

  public void note(CharSequence message) {
    if (DEBUG) {
      processingEnv
          .getMessager()
          .printMessage(Kind.NOTE, "[%s] %s".formatted(getTimestamp(), message));
    }
  }

  public VajramValidationException errorAndThrow(String message, @Nullable Element element) {
    error(message, element);
    return new VajramValidationException(message);
  }

  public void error(String message, @Nullable Element element) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
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

  public static String getImmutRequestProtoName(String vajramName) {
    return vajramName + Constants.IMMUT_REQUEST_PROTO_SUFFIX;
  }

  public static String getProtoFileName(String vajramName) {
    return vajramName + Constants.PROTO_FILE_SUFFIX;
  }

  public static String getVajramImplClassName(String vajramId) {
    return vajramId + Constants.IMPL_SUFFIX;
  }

  public static String getFacetsInterfaceName(String vajramName) {
    return vajramName + Constants.FACETS_CLASS_SUFFIX;
  }

  public static String getImmutFacetsClassname(String vajramName) {
    return vajramName + IMMUT_FACETS_CLASS_SUFFIX;
  }

  public TypeName toTypeName(DataType<?> dataType) {
    return TypeName.get(toTypeMirror(dataType));
  }

  public TypeMirror toTypeMirror(DataType<?> dataType) {
    return dataType.javaModelType(processingEnv);
  }

  public static TypeName toTypeName(Type typeArg) {
    if (typeArg instanceof ParameterizedType parameterizedType) {
      final Type rawType = parameterizedType.getRawType();
      final Type[] typeArgs = parameterizedType.getActualTypeArguments();
      return ParameterizedTypeName.get(
          (ClassName) toTypeName(rawType),
          Arrays.stream(typeArgs).map(Utils::toTypeName).toArray(TypeName[]::new));
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
   * @param className fully qualifield class name
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder classBuilder(String className) {
    TypeSpec.Builder classBuilder;
    if (className.isBlank()) {
      classBuilder = TypeSpec.anonymousClassBuilder("");
    } else {
      classBuilder = TypeSpec.classBuilder(className);
    }
    return addDefaultAnnotations(classBuilder);
  }

  private TypeSpec.Builder addDefaultAnnotations(TypeSpec.Builder classBuilder) {
    return classBuilder
        .addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember(
                    "value",
                    List.of(
                            CodeBlock.of("$S", "unchecked"),
                            CodeBlock.of("$S", "ClassReferencesSubclass"))
                        .stream()
                        .collect(joining(",", "{", "}")))
                .build())
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

  /**
   * Creates a class builder with the given class name. If the interfaceName is a blank string, then
   * the builder represents an anonymous class.
   *
   * @param interfaceName fully qualifield class name
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder interfaceBuilder(String interfaceName) {
    TypeSpec.Builder interfaceBuilder;
    if (interfaceName.isBlank()) {
      throw new RuntimeException("interface name connot be blank");
    } else {
      interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName);
    }
    return addDefaultAnnotations(interfaceBuilder);
  }

  public TypeAndName box(TypeAndName javaType, AnnotationSpec... annotationSpecs) {
    List<AnnotationSpec> annotationSpecList =
        Streams.concat(javaType.annotationSpecs().stream(), stream(annotationSpecs))
            .distinct()
            .toList();
    @Nullable TypeMirror typeMirror = javaType.type();
    if (typeMirror == null || !typeMirror.getKind().isPrimitive()) {
      return new TypeAndName(javaType.typeName(), null, annotationSpecList);
    }
    TypeMirror boxed = processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType();
    return new TypeAndName(
        TypeName.get(boxed).annotated(annotationSpecList), boxed, annotationSpecList);
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

  TypeName responsesType(DependencyModel dep) {
    return responsesType(
        new TypeAndName(toClassName(dep.depReqClassQualifiedName())), getTypeName(dep.dataType()));
  }

  private TypeName responsesType(TypeAndName requestType, TypeAndName facetType) {
    return ParameterizedTypeName.get(
        ClassName.get(FanoutDepResponses.class), requestType.typeName(), box(facetType).typeName());
  }

  TypeAndName getTypeName(DataType<?> dataType, List<AnnotationSpec> typeAnnotations) {
    TypeMirror javaModelType = dataType.javaModelType(processingEnv);
    return new TypeAndName(
        TypeName.get(javaModelType).annotated(typeAnnotations), javaModelType, typeAnnotations);
  }

  public TypeAndName getTypeName(DataType<?> dataType) {
    return getTypeName(dataType, List.of());
  }

  public DataType<?> getDataType(FacetGenModel abstractInput) {
    if (abstractInput instanceof GivenFacetModel facetDef) {
      return facetDef.dataType();
    } else if (abstractInput instanceof DependencyModel dep) {
      return dep.dataType();
    } else {
      throw new UnsupportedOperationException(
          "Unable to extract datatype from facet : %s".formatted(abstractInput));
    }
  }

  @SuppressWarnings("method.invocation")
  public ExecutableElement getMethodToOverride(Class<?> clazz, String methodName, int paramCount) {
    return checkNotNull(
            processingEnv()
                .getElementUtils()
                .getTypeElement(checkNotNull(clazz.getCanonicalName())))
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
        // Fanout dependency
        return new FanoutResponses(this);
      } else {
        return new One2OneResponse(this);
      }
    } else if (facet.isMandatory()) {
      return new Actual(this);
    } else {
      return new Boxed(this);
    }
  }

  public TypeName wrapWithFacetValueClass(DependencyModel dep) {
    return dep.canFanout() ? responsesType(dep) : responseType(dep);
  }

  public String getJavaTypeCreationCode(
      JavaType<?> javaType, List<TypeName> collectClassNames, VariableElement facetField) {
    TypeMirror typeMirror = javaType.javaModelType(processingEnv);
    collectClassNames.add(ClassName.get(JavaType.class));
    if (javaType.typeParameters().isEmpty()) {
      collectClassNames.add(TypeName.get(typeMirror));
      return "$T.create($T.class)";
    } else {
      collectClassNames.add(TypeName.get(processingEnv.getTypeUtils().erasure(typeMirror)));
      collectClassNames.add(ClassName.get(List.class));
      return "$T.create($T.class, $T.of("
          + javaType.typeParameters().stream()
              .map(
                  dataType -> {
                    if (!(dataType instanceof JavaType<?> typeParamType)) {
                      error("Unrecognised data type %s".formatted(dataType), facetField);
                      return "";
                    } else {
                      return getJavaTypeCreationCode(typeParamType, collectClassNames, facetField);
                    }
                  })
              .collect(Collectors.joining(","))
          + "))";
    }
  }
}
