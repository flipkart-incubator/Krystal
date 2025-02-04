package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants._FACETS_CLASS;
import static com.flipkart.krystal.vajram.codegen.DeclaredTypeVisitor.getMandatoryTag;
import static com.flipkart.krystal.vajram.utils.Constants.IMMUT_FACETS_CLASS_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableBiMap.toImmutableBiMap;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.Generated;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.codegen.FacetJavaType.Actual;
import com.flipkart.krystal.vajram.codegen.FacetJavaType.Boxed;
import com.flipkart.krystal.vajram.codegen.FacetJavaType.FanoutResponses;
import com.flipkart.krystal.vajram.codegen.FacetJavaType.One2OneResponse;
import com.flipkart.krystal.vajram.codegen.FacetJavaType.OptionalType;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.FacetGenModel;
import com.flipkart.krystal.vajram.codegen.models.GivenFacetModel;
import com.flipkart.krystal.vajram.codegen.models.GivenFacetModel.GivenFacetModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.exception.VajramDefinitionException;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FacetId;
import com.flipkart.krystal.vajram.facets.FacetIdNameMapping;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.flipkart.krystal.vajram.facets.ReservedFacets;
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
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
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
import org.checkerframework.checker.nullness.qual.Nullable;

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
              Facets.class,
              Facets.class + " is not an allowed facet type as this can cause undesired behaviour.")
          .build();

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

  static ClassName toClassName(String depReqClassName) {
    int lastDotIndex = depReqClassName.lastIndexOf('.');
    return ClassName.get(
        depReqClassName.substring(0, lastDotIndex), depReqClassName.substring(lastDotIndex + 1));
  }

  static List<AnnotationSpec> recordAnnotations() {
    return List.of(
        AnnotationSpec.builder(EqualsAndHashCode.class).build(),
        AnnotationSpec.builder(ToString.class).addMember("doNotUseGetters", "true").build());
  }

  static List<AnnotationSpec> annotations(Class<?>... annotations) {
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
    return Optional.ofNullable(processingEnv.getElementUtils().getTypeElement(name))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Could not find type element with name %s".formatted(name)));
  }

  List<TypeElement> getVajramClasses(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
        .toList();
  }

  VajramCodeGenerator createCodeGenerator(VajramInfo vajramInfo) {
    return new VajramCodeGenerator(vajramInfo, this);
  }

  void generateSourceFile(String className, String code, TypeElement vajramDefinition) {
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
    Optional<Element> facetsClass =
        vajramClass.getEnclosedElements().stream()
            .filter(element -> element.getKind() == ElementKind.CLASS)
            .filter(element -> element.getSimpleName().contentEquals(_FACETS_CLASS))
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
                        || variableElement.getAnnotation(Inject.class) != null)
            .toList();
    List<VariableElement> dependencyFields =
        fields.stream()
            .filter(variableElement -> variableElement.getAnnotation(Dependency.class) != null)
            .toList();
    PackageElement enclosingElement = (PackageElement) vajramClass.getEnclosingElement();
    String packageName = enclosingElement.getQualifiedName().toString();
    AtomicInteger nextFacetId = new AtomicInteger(1);
    VajramInfo vajramInfo =
        new VajramInfo(
            vajramInfoLite.vajramId(),
            vajramInfoLite.responseType(),
            packageName,
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
                .collect(toImmutableList()),
            ImmutableBiMap.copyOf(givenIdsByName),
            vajramClass);
    note("VajramInfo: %s".formatted(vajramInfo));
    return vajramInfo;
  }

  private GivenFacetModel toInputModel(
      VariableElement inputField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId,
      VajramInfoLite vajramInfoLite) {
    GivenFacetModelBuilder<Object> inputBuilder = GivenFacetModel.builder().facetField(inputField);
    String facetName = inputField.getSimpleName().toString();
    inputBuilder.id(
        Optional.ofNullable(givenIdsByName.get(facetName))
            .orElseGet(() -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    inputBuilder.name(facetName);
    inputBuilder.documentation(
        Optional.ofNullable(elementUtils.getDocComment(inputField)).orElse(""));
    inputBuilder.mandatoryAnno(getMandatoryTag(inputField));
    DataType<Object> dataType =
        inputField.asType().accept(new DeclaredTypeVisitor<>(this, inputField), null);
    inputBuilder.dataType(dataType);
    inputBuilder.isBatched(Optional.ofNullable(inputField.getAnnotation(Batch.class)).isPresent());
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
        Optional.ofNullable(givenIdsByName.get(facetName))
            .orElseGet(() -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    depBuilder.name(facetName);
    depBuilder.mandatoryAnno(getMandatoryTag(depField));
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
                            getTypeElement(Vajram.class.getName(), processingEnv)
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
    depBuilder.documentation(Optional.ofNullable(elementUtils.getDocComment(depField)).orElse(""));
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      error(
          ("Both `withVajramReq` and `onVajram` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'."
                  + " Found withVajramReq=%s and onVajram=%s")
              .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
          depField);
    } else {
      DataType<?> declaredDataType =
          new DeclaredTypeVisitor<>(this, depField).visit(depField.asType());
      TypeElement vajramOrReqElement =
          checkNotNull((TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType));
      VajramInfoLite depVajramInfoLite = getVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramInfoLite(depVajramInfoLite)
          .depReqClassQualifiedName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!declaredDataType.equals(depVajramInfoLite.responseType())) {
        error(
            "Declared dependency type %s does not match dependency vajram response type %s"
                .formatted(declaredDataType, depVajramInfoLite.responseType()),
            depField);
      }
      DependencyModel depModel =
          depBuilder
              .dataType(declaredDataType)
              .isBatched(Optional.ofNullable(depField.getAnnotation(Batch.class)).isPresent())
              .vajramInfo(depVajramInfoLite)
              .build();
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
    PackageElement packageName = elementUtils.getPackageOf(vajramOrReqClass);
    ImmutableBiMap<Integer, String> facetIdNameMappings = ImmutableBiMap.of();
    if (isRawAssignable(vajramOrReqClass.asType(), Request.class)) {
      facetIdNameMappings =
          vajramOrReqClass.getEnclosedElements().stream()
              .map(element -> element.getAnnotation(FacetIdNameMapping.class))
              .filter(Objects::nonNull)
              .collect(toImmutableBiMap(FacetIdNameMapping::id, FacetIdNameMapping::name));
      TypeMirror responseType = getResponseType(vajramOrReqClass, Request.class);
      TypeElement responseTypeElement =
          checkNotNull((TypeElement) typeUtils.asElement(responseType));
      return new VajramInfoLite(
          packageName.getQualifiedName().toString(),
          vajramID(
              vajramClassSimpleName.substring(
                  0, vajramClassSimpleName.length() - Constants.REQUEST_SUFFIX.length())),
          new DeclaredTypeVisitor<>(this, responseTypeElement).visit(responseType),
          facetIdNameMappings);
    } else if (isRawAssignable(vajramOrReqClass.asType(), Vajram.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, Vajram.class);
      TypeElement responseTypeElement =
          checkNotNull((TypeElement) typeUtils.asElement(responseType));
      TypeElement requestType =
          elementUtils.getTypeElement(
              packageName.getQualifiedName()
                  + "."
                  + getRequestInterfaceName(vajramClassSimpleName));
      if (requestType != null) {
        facetIdNameMappings =
            requestType.getEnclosedElements().stream()
                .map(element -> element.getAnnotation(FacetIdNameMapping.class))
                .filter(Objects::nonNull)
                .collect(toImmutableBiMap(FacetIdNameMapping::id, FacetIdNameMapping::name));
      }
      VajramDef vajramDef = vajramOrReqClass.getAnnotation(VajramDef.class);
      if (vajramDef == null) {
        throw new VajramValidationException(
            "Vajram class %s does not have @VajramDef annotation. This should not happen"
                .formatted(vajramOrReqClass));
      } else if (!vajramDef.id().isEmpty()) {
        throw new VajramValidationException(
            "'id' cannot be explicitly mentioned. It will be auto-inferred from the class name");
      }
      VajramID vajramId = vajramID(vajramClassSimpleName);
      return new VajramInfoLite(
          packageName.getQualifiedName().toString(),
          vajramId,
          new DeclaredTypeVisitor<>(this, responseTypeElement).visit(responseType),
          facetIdNameMappings);
    } else {
      throw new IllegalArgumentException(
          "Unknown class hierarchy of vajram class %s. Expected %s or %s"
              .formatted(vajramOrReqClass, Vajram.class, ImmutableRequest.class));
    }
  }

  private String getVajramReqClassName(TypeElement vajramClass) {
    if (isRawAssignable(vajramClass.asType(), Vajram.class)) {
      return vajramClass.getQualifiedName().toString() + Constants.REQUEST_SUFFIX;
    } else if (isRawAssignable(vajramClass.asType(), ImmutableRequest.class)) {
      return vajramClass.getQualifiedName().toString();
    } else {
      throw new AssertionError("This should not happen!");
    }
  }

  private Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> runnable) {
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

  void note(CharSequence message) {
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
    String ist = "IST";
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        Clock.system(
                ZoneId.of(
                    Optional.ofNullable(ZoneId.SHORT_IDS.get(ist))
                        .orElseThrow(() -> new IllegalStateException("Could not find Zone" + ist))))
            .instant());
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
    return vajramName + FACETS_CLASS_SUFFIX;
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
    return classBuilder.addAnnotation(
        AnnotationSpec.builder(Generated.class).addMember("by", "$S", generator.getName()).build());
  }

  TypeAndName box(TypeAndName javaType, AnnotationSpec... annotationSpecs) {
    List<AnnotationSpec> annotationSpecList =
        Streams.concat(javaType.annotationSpecs().stream(), stream(annotationSpecs))
            .distinct()
            .toList();
    Optional<TypeMirror> typeMirror = javaType.type();
    if (typeMirror.isEmpty() || !typeMirror.get().getKind().isPrimitive()) {
      return new TypeAndName(javaType.typeName(), Optional.empty(), annotationSpecList);
    }
    TypeMirror boxed =
        processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror.get()).asType();
    return new TypeAndName(
        TypeName.get(boxed).annotated(annotationSpecList), Optional.of(boxed), annotationSpecList);
  }

  TypeName optional(TypeAndName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), box(javaType).typeName());
  }

  TypeName errable(TypeAndName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Errable.class), box(javaType).typeName());
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
        TypeName.get(javaModelType).annotated(typeAnnotations),
        Optional.of(javaModelType),
        typeAnnotations);
  }

  TypeAndName getTypeName(DataType<?> dataType) {
    return getTypeName(dataType, List.of());
  }

  DataType<?> getDataType(FacetGenModel abstractInput) {
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
  ExecutableElement getMethodToOverride(Class<?> clazz, String methodName, int paramCount) {
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
        .orElseThrow();
  }

  FacetJavaType getReturnType(FacetGenModel facet, CodeGenParams codeGenParams) {
    if (facet instanceof DependencyModel dep) {
      if (dep.canFanout()) {
        return new FanoutResponses(this);
      } else {
        return new One2OneResponse(this);
      }
    } else {
      boolean devAccessible = codeGenParams.isDevAccessible() && codeGenParams.isLocal();
      Mandatory mandatoryAnno = facet.mandatoryAnno();
      if (mandatoryAnno != null
          && (mandatoryAnno.ifNotSet().usePlatformDefault() || devAccessible)) {
        return new Actual(this);
      } else if (devAccessible) {
        return new OptionalType(this);
      }
      return new Boxed(this);
    }
  }

  FacetJavaType getFacetFieldType(FacetGenModel facet) {
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

  TypeName wrapWithFacetValueClass(DependencyModel dep) {
    return dep.canFanout() ? responsesType(dep) : responseType(dep);
  }

  String getJavaTypeCreationCode(
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
              .collect(joining(","))
          + "))";
    }
  }
}
