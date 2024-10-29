package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.codegen.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.Constants.IMMUT_FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.DeclaredTypeVisitor.isOptional;
import static com.flipkart.krystal.vajram.utils.Constants.FACETS_CLASS_NAME_SUFFIX;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.Generated;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.batching.Batch;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel;
import com.flipkart.krystal.vajram.codegen.models.DependencyModel.DependencyModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.InputModel;
import com.flipkart.krystal.vajram.codegen.models.InputModel.InputModelBuilder;
import com.flipkart.krystal.vajram.codegen.models.VajramInfo;
import com.flipkart.krystal.vajram.codegen.models.VajramInfoLite;
import com.flipkart.krystal.vajram.exception.VajramValidationException;
import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.FacetId;
import com.flipkart.krystal.vajram.facets.Input;
import com.flipkart.krystal.vajram.facets.ReservedFacets;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
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
import lombok.Getter;

public class Utils {

  private static final boolean DEBUG = false;

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

  List<TypeElement> getVajramClasses(RoundEnvironment roundEnv) {
    return roundEnv.getElementsAnnotatedWith(VajramDef.class).stream()
        .filter(element -> element.getKind() == ElementKind.CLASS)
        .map(executableElement -> (TypeElement) executableElement)
        .toList();
  }

  VajramCodeGenerator createCodeGenerator(VajramInfo vajramInfo) {
    Map<VajramID, VajramInfoLite> vajramDefs = new HashMap<>();
    for (DependencyModel depModel : vajramInfo.dependencies()) {
      vajramDefs.put(
          depModel.depVajramId(),
          new VajramInfoLite(depModel.depVajramId().vajramId(), depModel.dataType()));
    }
    return new VajramCodeGenerator(vajramInfo, vajramDefs, processingEnv, this);
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
            .filter(element -> element.getSimpleName().contentEquals(FACETS_CLASS_NAME_SUFFIX))
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
            vajramID(vajramInfoLite.vajramId()),
            vajramInfoLite.responseType(),
            packageName,
            inputFields.stream()
                .map(
                    inputField ->
                        toInputModel(inputField, givenIdsByName, takenFacetIds, nextFacetId))
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

  private InputModel<Object> toInputModel(
      VariableElement inputField,
      BiMap<String, Integer> givenIdsByName,
      Set<Integer> takenFacetIds,
      AtomicInteger nextFacetId) {
    InputModelBuilder<Object> inputBuilder = InputModel.builder().facetField(inputField);
    String facetName = inputField.getSimpleName().toString();
    inputBuilder.id(
        Optional.ofNullable(givenIdsByName.get(facetName))
            .orElseGet(() -> getNextAvailableFacetId(takenFacetIds, nextFacetId)));
    inputBuilder.name(facetName);
    inputBuilder.isMandatory(!isOptional(inputField.asType(), processingEnv));
    DataType<Object> dataType =
        inputField.asType().accept(new DeclaredTypeVisitor<>(this, true, inputField), null);
    inputBuilder.dataType(dataType);
    inputBuilder.isBatched(Optional.ofNullable(inputField.getAnnotation(Batch.class)).isPresent());
    EnumSet<FacetType> facetTypes = EnumSet.noneOf(FacetType.class);
    if (inputField.getAnnotation(Input.class) != null) {
      facetTypes.add(FacetType.INPUT);
    }
    if (inputField.getAnnotation(Inject.class) != null) {
      facetTypes.add(FacetType.INJECTION);
    }
    InputModel<Object> inputModel = inputBuilder.facetTypes(facetTypes).build();
    givenIdsByName.putIfAbsent(facetName, inputModel.id());
    return inputModel;
  }

  private static int getNextAvailableFacetId(
      Set<Integer> takenFacetIds, AtomicInteger nextFacetId) {
    while (takenFacetIds.contains(nextFacetId.get())) {
      nextFacetId.getAndIncrement();
    }
    return nextFacetId.getAndIncrement();
  }

  private DependencyModel toDependencyModel(
      String vajramId,
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
    depBuilder.isMandatory(!isOptional(depField.asType(), processingEnv));
    Optional<TypeMirror> vajramReqType =
        getTypeFromAnnotationMember(dependency::withVajramReq)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(
                            getTypeElement(ImmutableRequest.class.getName()).getQualifiedName()));
    Optional<TypeMirror> vajramType =
        getTypeFromAnnotationMember(dependency::onVajram)
            .filter(
                typeMirror ->
                    !((QualifiedNameable) typeUtils.asElement(typeMirror))
                        .getQualifiedName()
                        .equals(getTypeElement(Vajram.class.getName()).getQualifiedName()));
    TypeMirror vajramOrReqType =
        vajramReqType
            .or(() -> vajramType)
            .orElseThrow(
                () -> {
                  error(
                      "At least one of `onVajram` or `withVajramReq` is needed in dependency declaration '%s' of vajram '%s'"
                          .formatted(depField.getSimpleName(), vajramId),
                      depField);
                  return new RuntimeException("Invalid Dependency specification");
                });
    if (vajramReqType.isPresent() && vajramType.isPresent()) {
      error(
          ("Both `withVajramReq` and `onVajram` cannot be set."
                  + " Please set only one of them for dependency '%s' of vajram '%s'."
                  + " Found withVajramReq=%s and onVajram=%s")
              .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
          depField);
    } else {
      DataType<?> declaredDataType =
          new DeclaredTypeVisitor<>(this, true, depField).visit(depField.asType());
      TypeElement vajramOrReqElement =
          (TypeElement) processingEnv.getTypeUtils().asElement(vajramOrReqType);
      VajramInfoLite depVajramId = getVajramInfoLite(vajramOrReqElement);
      depBuilder
          .depVajramId(vajramID(depVajramId.vajramId()))
          .depReqClassQualifiedName(getVajramReqClassName(vajramOrReqElement))
          .canFanout(dependency.canFanout());
      if (!declaredDataType.equals(depVajramId.responseType())) {
        error(
            "Declared dependency type %s does not match dependency vajram response type %s"
                .formatted(declaredDataType, depVajramId.responseType()),
            depField);
      }
      depBuilder.dataType(declaredDataType);
      depBuilder.isBatched(Optional.ofNullable(depField.getAnnotation(Batch.class)).isPresent());
      DependencyModel depModel = depBuilder.build();
      givenIdsByName.putIfAbsent(facetName, depModel.id());
      return depModel;
    }
    throw errorAndThrow(
        ("Invalid dependency spec of dependency '%s' of vajram '%s'."
                + " Found withVajramReq=%s and onVajram=%s")
            .formatted(depField.getSimpleName(), vajramId, vajramReqType.get(), vajramType.get()),
        depField);
  }

  public TypeElement getTypeElement(String name) {
    return Optional.ofNullable(elementUtils.getTypeElement(name))
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Could not find type element with name %s".formatted(name)));
  }

  private VajramInfoLite getVajramInfoLite(TypeElement vajramOrReqClass) {
    String vajramClassSimpleName = vajramOrReqClass.getSimpleName().toString();
    if (isRawAssignable(vajramOrReqClass.asType(), Request.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, Request.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      return new VajramInfoLite(
          vajramClassSimpleName.substring(
              0, vajramClassSimpleName.length() - Constants.REQUEST_SUFFIX.length()),
          new DeclaredTypeVisitor<>(this, false, responseTypeElement).visit(responseType));
    } else if (isRawAssignable(vajramOrReqClass.asType(), Vajram.class)) {
      TypeMirror responseType = getResponseType(vajramOrReqClass, Vajram.class);
      TypeElement responseTypeElement = (TypeElement) typeUtils.asElement(responseType);
      VajramDef vajramDef = vajramOrReqClass.getAnnotation(VajramDef.class);
      if (vajramDef == null) {
        throw new IllegalArgumentException(
            "Vajram class %s does not have @VajramDef annotation. This should not happen"
                .formatted(vajramOrReqClass));
      }
      return new VajramInfoLite(
          vajramClassSimpleName,
          new DeclaredTypeVisitor<>(this, false, responseTypeElement).visit(responseType));
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
      runnable.get();
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
    return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
        Clock.systemDefaultZone().instant().atZone(ZoneId.systemDefault()));
  }

  public static String getRequestInterfaceName(String vajramName) {
    return vajramName + Constants.REQUEST_SUFFIX;
  }

  public static String getImmutRequestClassName(String vajramName) {
    return vajramName + Constants.IMMUT_REQUEST_SUFFIX;
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

  //  public static String getCommonFacetsInterfaceName(String vajramName) {
  //    return vajramName + COMMON_FACETS;
  //  }
  //
  //  public static String getCommonImmutFacetsClassname(String vajramName) {
  //    return vajramName + COMMON_IMMUT_FACETS_CLASS_SUFFIX;
  //  }
  //
  //  public static String getBatchFacetsInterfaceName(String vajramName) {
  //    return vajramName + BATCH_FACETS;
  //  }
  //
  //  public static String getBatchImmutFacetsClassname(String vajramName) {
  //    return vajramName + BATCH_IMMUT_FACETS_CLASS_SUFFIX;
  //  }

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
   * @return true of the raw type (without generics) of {@code from} can be assigned to the raw type
   *     of {@code to}
   */
  public boolean isRawAssignable(TypeMirror from, Class<?> to) {
    return typeUtils.isAssignable(
        typeUtils.erasure(from), typeUtils.erasure(getTypeElement(to.getName()).asType()));
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
}
