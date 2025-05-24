package com.flipkart.krystal.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.ABSTRACT;

import com.flipkart.krystal.annos.Generated;
import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeRegistry;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.Creator;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.ModelRoot;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
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

  private static final boolean DEBUG = true;

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

  public static String capitalizeFirstChar(String str) {
    return str.isEmpty() ? str : Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }

  public static String lowerCaseFirstChar(String str) {
    return str.isEmpty() ? str : Character.toLowerCase(str.charAt(0)) + str.substring(1);
  }

  public ClassName toClassName(String depReqClassName) {
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

  public void generateSourceFile(String className, String code, TypeElement originatingElement) {
    try {
      JavaFileObject requestFile =
          processingEnv.getFiler().createSourceFile(className, originatingElement);
      note("Successfully Create source file %s".formatted(className));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.println(code);
      }
    } catch (Exception e) {
      error(
          "Error creating java file for className: %s. Error: %s".formatted(className, e),
          originatingElement);
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

  public ImmutableList<TypeMirror> getTypeParamTypes(
      TypeElement childTypeElement, TypeElement targetParentClass) {
    List<TypeMirror> currentTypes = List.of(childTypeElement.asType());
    note("Child Type Element: %s".formatted(childTypeElement));

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
    addDefaultAnnotations(classBuilder);
    return classBuilder;
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

  /**
   * Creates a class builder with the given class name. If the interfaceName is a blank string, then
   * the builder represents an anonymous class.
   *
   * @param interfaceName fully qualified class name
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder interfaceBuilder(String interfaceName) {
    TypeSpec.Builder interfaceBuilder;
    if (interfaceName.isBlank()) {
      throw new RuntimeException("interface name cannot be blank");
    } else {
      interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName);
    }
    addDefaultAnnotations(interfaceBuilder);
    return interfaceBuilder;
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

  public TypeAndName getTypeName(CodeGenType dataType, List<AnnotationSpec> typeAnnotations) {
    TypeMirror javaModelType = dataType.javaModelType(processingEnv);
    return new TypeAndName(
        TypeName.get(javaModelType).annotated(typeAnnotations), javaModelType, typeAnnotations);
  }

  public TypeAndName getTypeName(CodeGenType dataType) {
    return getTypeName(dataType, List.of());
  }

  @SuppressWarnings("method.invocation")
  public ExecutableElement getMethod(Class<?> clazz, String methodName, int paramCount) {
    return getMethod(
            requireNonNull(
                processingEnv()
                    .getElementUtils()
                    .getTypeElement(requireNonNull(clazz.getCanonicalName()))),
            methodName,
            paramCount)
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

  public Optional<ExecutableElement> getMethod(
      TypeElement typeElement, String methodName, int paramCount) {
    return typeElement.getEnclosedElements().stream()
        .filter(element -> element instanceof ExecutableElement)
        .map(element -> (ExecutableElement) element)
        .filter(
            element ->
                element.getSimpleName().contentEquals(methodName)
                    && element.getParameters().size() == paramCount)
        .findAny();
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

  public TypeName optional(TypeAndName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), box(javaType).typeName());
  }

  public ClassName getImmutClassName(TypeElement modelRootType) {
    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    if (modelRoot == null) {
      throw new IllegalArgumentException(
          "Cannot fetch Immut class name for Model which does not have @ModelRoot annotation");
    }

    String modelRootName = modelRootType.getSimpleName().toString();
    String packageName = processingEnv().getElementUtils().getPackageOf(modelRootType).toString();
    return ClassName.get(packageName, modelRootName + modelRoot.suffixSeparator() + IMMUT_SUFFIX);
  }
}
