package com.flipkart.krystal.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.annos.Generated;
import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeRegistry;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.Creator;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.TypeVariableName;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
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
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
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
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassWithTooManyMethods")
@Slf4j
public class CodeGenUtility {

  private static final boolean NOTE_LEVEL =
      System.getProperty(Constants.LOG_LEVEL, "error").equalsIgnoreCase("note");

  @Getter private final ProcessingEnvironment processingEnv;
  private final Types typeUtils;
  private final Elements elementUtils;
  private final Class<?> generator;
  @Getter private final @Nullable CodegenPhase codegenPhase;
  @Getter private final DataTypeRegistry dataTypeRegistry;
  @Getter private final @Nullable Path moduleRootPath;

  public CodeGenUtility(
      ProcessingEnvironment processingEnv,
      Class<?> generator,
      @Nullable CodegenPhase codegenPhase) {
    this.processingEnv = processingEnv;
    this.typeUtils = processingEnv.getTypeUtils();
    this.elementUtils = processingEnv.getElementUtils();
    this.generator = generator;
    this.codegenPhase = codegenPhase;
    this.dataTypeRegistry = new DataTypeRegistry();
    String moduleRootOption = processingEnv.getOptions().get(Constants.MODULE_ROOT_PATH_KEY);
    this.moduleRootPath = moduleRootOption != null ? Paths.get(moduleRootOption) : null;
  }

  public static String capitalizeFirstChar(String str) {
    return str.isEmpty() ? str : Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }

  public static String lowerCaseFirstChar(String str) {
    return str.isEmpty() ? str : Character.toLowerCase(str.charAt(0)) + str.substring(1);
  }

  public String getPackageName(Element element) {
    return requireNonNull(processingEnv().getElementUtils().getPackageOf(element))
        .getQualifiedName()
        .toString();
  }

  @SneakyThrows
  public void addImmutableModelObjectMethods(
      ClassName immutInterfaceName,
      Set<? extends CharSequence> modelFieldNames,
      Builder classBuilder) {
    addCommonObjectMethods(classBuilder);

    classBuilder.addMethod(
        MethodSpec.overriding(getMethod(Object.class, "equals", 1))
            .addCode(
                """
                if (this == obj) {
                  return true;
                }
                if (!(obj instanceof $T other)) {
                  return false;
                }
                return $L;
                """,
                immutInterfaceName,
                modelFieldNames.isEmpty()
                    ? "true"
                    : modelFieldNames.stream()
                        .map(
                            name ->
                                CodeBlock.of(
                                    "$T.equals(this.$L(), other.$L())", Objects.class, name, name))
                        .collect(joining("\n&& ")))
            .build());

    classBuilder.addField(int.class, "_memoizedHashCode", PRIVATE);
    classBuilder.addMethod(
        MethodSpec.methodBuilder("hashCode")
            .addModifiers(PUBLIC)
            .returns(int.class)
            .addAnnotation(Override.class)
            .addCode(
                "if(_memoizedHashCode == 0) { _memoizedHashCode =  $T.hash($L); }",
                Objects.class,
                modelFieldNames.stream()
                    .map(Object::toString)
                    .sorted()
                    .map(name -> CodeBlock.of("this.$L()", name))
                    .collect(CodeBlock.joining(",\n")))
            .addStatement("return _memoizedHashCode")
            .build());
  }

  public static void addCommonObjectMethods(Builder classBuilder) {
    classBuilder.addAnnotation(
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

  public boolean isAnyNullable(TypeMirror type, Element elementToCheck) {
    return isAnyNullable(type::getAnnotationMirrors)
        || isAnyNullable(elementToCheck::getAnnotationMirrors);
  }

  private boolean isAnyNullable(Supplier<List<? extends AnnotationMirror>> annotationMirrors) {
    return annotationMirrors.get().stream()
        .map(AnnotationMirror::getAnnotationType)
        .map(DeclaredType::asElement)
        .anyMatch(element -> element.getSimpleName().contentEquals("Nullable"));
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
            processingEnv().getElementUtils().getTypeElement(Object.class.getCanonicalName()))
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

  public void generateSourceFile(
      String canonicalClassName, JavaFile code, TypeElement originatingElement) {
    StringWriter writer = new StringWriter();
    try {
      code.writeTo(writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    generateSourceFile(canonicalClassName, writer.toString(), originatingElement);
  }

  public void generateSourceFile(
      String canonicalClassName, String code, @Nullable TypeElement originatingElement) {
    try {
      JavaFileObject requestFile =
          processingEnv
              .getFiler()
              .createSourceFile(
                  canonicalClassName,
                  Optional.ofNullable(originatingElement).stream().toArray(Element[]::new));
      note("Successfully created source file %s".formatted(canonicalClassName));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.println(code);
      }
    } catch (Exception e) {
      error(
          "Error creating java file for className: %s. Error: %s".formatted(canonicalClassName, e),
          originatingElement);
    }
  }

  public Optional<TypeMirror> getTypeFromAnnotationMember(Supplier<Class<?>> supplier) {
    try {
      var ignored = supplier.get();
      throw new AssertionError("Expected supplier to throw error");
    } catch (MirroredTypeException mte) {
      TypeMirror typeMirror = mte.getTypeMirror();
      return Optional.ofNullable(typeMirror);
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
          Element element = typeUtils.asElement(superType);
          if (element instanceof TypeElement typeElement) {
            if (typeElement
                .getQualifiedName()
                .contentEquals(targetParentClass.getQualifiedName())) {
              targetType = superType;
              break;
            }
          }
        }
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
    return targetType.getTypeArguments();
  }

  public void note(CharSequence message) {
    _note(message, null);
  }

  public void note(CharSequence message, @Nullable TypeElement typeElement) {
    _note(message, typeElement);
  }

  private void _note(CharSequence message, @Nullable TypeElement typeElement) {
    if (NOTE_LEVEL) {
      processingEnv
          .getMessager()
          .printMessage(
              Kind.NOTE,
              "[%s] [%s] %s".formatted(getCallerInfo(), String.valueOf(codegenPhase), message),
              typeElement);
    }
  }

  public CodeValidationException errorAndThrow(String message, @Nullable Element... elements) {
    _error(message, elements);
    return new CodeValidationException(message);
  }

  public void error(String message, @Nullable Element... elements) {
    _error(message, elements);
  }

  private void _error(String message, @Nullable Element... elements) {
    String enrichedMessage =
        "[%s] [%s:%s] %s"
            .formatted(getCallerInfo(), this.generator, String.valueOf(codegenPhase), message);
    if (elements.length == 0) {
      processingEnv.getMessager().printMessage(Kind.ERROR, enrichedMessage);
    } else {
      for (Element element : elements) {
        processingEnv.getMessager().printMessage(Kind.ERROR, enrichedMessage, element);
      }
    }
  }

  private static String getCallerInfo() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    // stackTrace[0] = Thread.getStackTrace()
    // stackTrace[1] = CodeGenUtility.getCallerInfo()
    // stackTrace[2] = a private util method (_note/_error)
    // stackTrace[3] = a public util method
    // stackTrace[4 and above] = the actual callers
    if (stackTrace.length > 4) {
      StringBuilder callerInfo = new StringBuilder();
      for (int i = Math.min(5, stackTrace.length - 1); i >= 4; i--) {
        StackTraceElement stackTraceElement = stackTrace[i];
        String fullClassName = stackTraceElement.getClassName();
        callerInfo
            .append(fullClassName.substring((fullClassName.lastIndexOf('.') + 1)))
            .append(":")
            .append(stackTraceElement.getMethodName())
            .append(":")
            .append(stackTraceElement.getLineNumber())
            .append("=>");
      }
      return callerInfo.toString();
    } else {
      throw new AssertionError();
    }
  }

  private String getTimestamp() {
    return ISO_OFFSET_DATE_TIME.format(
        OffsetDateTime.now(ZoneId.of(checkNotNull(ZoneId.SHORT_IDS.get("IST")))));
  }

  public TypeName toTypeName(CodeGenType dataType) {
    return TypeName.get(dataType.javaModelType(processingEnv));
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
    } else if (type.getKind() == TypeKind.VOID) {
      return requireNonNull(
              elementUtils.getTypeElement(requireNonNull(Void.class.getCanonicalName())))
          .asType();
    } else {
      return type;
    }
  }

  private void addDefaultAnnotations(Builder classBuilder) {
    classBuilder.addAnnotation(
        AnnotationSpec.builder(SuppressWarnings.class)
            .addMember(
                "value",
                Stream.of(
                        CodeBlock.of("$S", "unchecked"),
                        CodeBlock.of("$S", "ClassReferencesSubclass"))
                    .collect(joining(",", "{", "}")))
            .build());
    classBuilder.addAnnotation(
        AnnotationSpec.builder(Accessors.class).addMember("fluent", "true").build());
    addGeneratedAnnotations(classBuilder);
  }

  public void addGeneratedAnnotations(Builder classBuilder) {
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

  public TypeSpec.Builder classBuilder(String simpleName, String generatedForCanonicalName) {
    return classBuilder(simpleName, List.of(), generatedForCanonicalName);
  }

  /**
   * Creates a class builder with the given class name. If the simpleName is a blank string, then
   * the builder represents an anonymous class.
   *
   * @param simpleName simple name of class
   * @param typeVariableNames Type variables of the interface, if any
   * @param generatedForCanonicalName canonical name of the originating class for which the class is
   *     being generated. Pass empty string if there is not such class.
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder classBuilder(
      String simpleName,
      List<TypeVariableName> typeVariableNames,
      String generatedForCanonicalName) {
    TypeSpec.Builder classBuilder;
    if (simpleName.isBlank()) {
      classBuilder = TypeSpec.anonymousClassBuilder("");
    } else {
      classBuilder = TypeSpec.classBuilder(simpleName);
    }
    if (!generatedForCanonicalName.isBlank()) {
      classBuilder.addJavadoc("@see $L", generatedForCanonicalName);
    }
    classBuilder.addTypeVariables(typeVariableNames);
    addDefaultAnnotations(classBuilder);
    return classBuilder;
  }

  public TypeSpec.Builder interfaceBuilder(String interfaceName, String generatedForCanonicalName) {
    return interfaceBuilder(interfaceName, List.of(), generatedForCanonicalName);
  }

  /**
   * Creates a class builder with the given class name. If the interfaceName is a blank string, then
   * the builder represents an anonymous class.
   *
   * @param interfaceName fully qualified class name
   * @param typeVariableNames Type variables of the interface, if any
   * @param generatedForCanonicalName canonical name of the originating class for which the
   *     interface is being generated
   * @return a class builder with the given class name, with the {@link Generated} annotation
   *     applied on the class
   */
  public TypeSpec.Builder interfaceBuilder(
      String interfaceName,
      List<TypeVariableName> typeVariableNames,
      String generatedForCanonicalName) {
    TypeSpec.Builder interfaceBuilder;
    if (interfaceName.isBlank()) {
      throw new RuntimeException("interface name cannot be blank");
    } else {
      interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName);
    }
    interfaceBuilder.addTypeVariables(typeVariableNames);
    addDefaultAnnotations(interfaceBuilder);
    if (!generatedForCanonicalName.isBlank()) {
      interfaceBuilder.addJavadoc("@see $L", generatedForCanonicalName);
    }
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

  @SneakyThrows
  public ExecutableElement getMethod(Callable<Method> methodSupplier) {
    Method method = methodSupplier.call();

    TypeElement typeElement =
        requireNonNull(
            processingEnv()
                .getElementUtils()
                .getTypeElement(requireNonNull(method.getDeclaringClass().getCanonicalName())));
    int parameterCount = method.getParameterCount();
    return typeElement.getEnclosedElements().stream()
        .filter(element -> element instanceof ExecutableElement)
        .map(element -> (ExecutableElement) element)
        .filter(
            element ->
                element.getSimpleName().contentEquals(method.getName())
                    && element.getParameters().size() == parameterCount
                    && IntStream.range(0, parameterCount)
                        .allMatch(
                            i ->
                                isSameRawType(
                                    element.getParameters().get(i).asType(),
                                    method.getParameterTypes()[i])))
        .findFirst()
        .orElseThrow(() -> new CodeGenerationException("Method " + method + " not found"));
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
    String packageName = getPackageName(modelRootType);
    return ClassName.get(packageName, modelRootName + modelRoot.suffixSeparator() + IMMUT_SUFFIX);
  }

  public ClassName getImmutSerdeClassName(TypeElement modelRootType, SerdeProtocol serdeProtocol) {
    ClassName immutClassName = getImmutClassName(modelRootType);
    return ClassName.get(
        immutClassName.packageName(),
        immutClassName.simpleName() + serdeProtocol.modelClassesSuffix());
  }

  /**
   * Writes a Java file to the source directory.
   *
   * @param packageName The package name for the file
   * @param typeSpec The type specification to write
   * @param originatingElement The element that caused this file to be generated
   */
  public void writeJavaFile(String packageName, TypeSpec typeSpec, TypeElement originatingElement) {
    try {
      JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
      String fileName = packageName + "." + typeSpec.name;
      generateSourceFile(fileName, javaFile.toString(), originatingElement);
    } catch (Exception e) {
      error("Error generating Java file: " + e.getMessage(), originatingElement);
    }
  }

  /**
   * Determines the parameter type for a method return type, handling Optional types.
   *
   * @param method The method
   * @param isBuilder is this a builder?
   * @return The appropriate parameter type
   */
  public TypeName getParameterType(ExecutableElement method, boolean isBuilder) {
    final TypeMirror specifiedType = method.getReturnType();
    TypeMirror inferredType = specifiedType;
    if (isOptional(specifiedType)) {
      // For Optional<T>, use T as the parameter type
      inferredType = getOptionalInnerType(specifiedType);
    }
    if (isBuilder && inferredType instanceof PrimitiveType primitiveType) {
      inferredType = typeUtils.boxedClass(primitiveType).asType();
    }
    TypeName typeName = inferredType.accept(new TypeNameVisitor(), null);
    // Add @Nullable annotation for Optional types or methods with @Nullable annotation
    if (isOptional(specifiedType)) {
      // Add @Nullable as a type annotation
      typeName = typeName.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
    }
    return typeName;
  }

  /** Returns true if the model root type supports the given model protocol. */
  public boolean typeExplicitlySupportsProtocol(
      TypeElement modelRootType, Class<? extends ModelProtocol> modelProtocol) {
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return false;
    }
    // Check if Json is mentioned in the annotation value
    return getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
        .map(typeMirror -> processingEnv().getTypeUtils().asElement(typeMirror))
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(s -> Objects.equals(s, modelProtocol.getCanonicalName()));
  }

  public static ClassName asClassName(TypeName typeName) {
    if (typeName instanceof ClassName className) {
      return className;
    } else if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
      return parameterizedTypeName.rawType;
    } else {
      throw new AssertionError();
    }
  }

  public static TypeName asTypeNameWithElements(
      ClassName className, List<? extends TypeParameterElement> typeParameterElements) {
    return asTypeNameWithTypes(
        className, typeParameterElements.stream().map(TypeParameterElement::asType).toList());
  }

  public static TypeName asTypeNameWithTypes(
      TypeName className, List<? extends TypeMirror> typeParams) {
    TypeNameVisitor typeNameVisitor = new TypeNameVisitor(true);
    List<? extends TypeName> typeNameStream =
        typeParams.stream().map(typeNameVisitor::visit).toList();
    return asTypeName(className, typeNameStream);
  }

  public static TypeName asTypeName(TypeName typeName, List<? extends TypeName> typeNameStream) {
    if (typeNameStream.isEmpty()) {
      return typeName;
    } else if (typeName instanceof ClassName className) {
      return ParameterizedTypeName.get(className, typeNameStream.toArray(TypeName[]::new));
    } else {
      throw new IllegalArgumentException(
          "If there are type names, the TypeName should be a ClassName");
    }
  }
}
