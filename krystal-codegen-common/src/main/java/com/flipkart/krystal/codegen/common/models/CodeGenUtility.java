package com.flipkart.krystal.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.WILL_NEVER_FAIL;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.squareup.javapoet.CodeBlock.joining;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.annos.Generated;
import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.DataTypeRegistry;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider;
import com.flipkart.krystal.codegen.common.spi.ModelProtocolConfigProvider.ModelProtocolConfig;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.Creator;
import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.model.SupportedModelProtocolName;
import com.flipkart.krystal.model.array.PrimitiveArray;
import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.serial.DefaultSerdeProtocol;
import com.flipkart.krystal.serial.DefaultSerdeProtocolName;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import com.google.googlejavaformat.java.GoogleJavaFormatTool;
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
import com.squareup.javapoet.WildcardTypeName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
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
  private final GoogleJavaFormatTool codeFormatter;
  private final TypeMirror objectType;

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
    this.codeFormatter = new GoogleJavaFormatTool();
    this.objectType =
        requireNonNull(
                processingEnv.getElementUtils().getTypeElement(Object.class.getCanonicalName()))
            .asType();
  }

  public static String capitalizeFirstChar(String str) {
    return str.isEmpty() ? str : Character.toUpperCase(str.charAt(0)) + str.substring(1);
  }

  public static String lowerCaseFirstChar(String str) {
    return str.isEmpty() ? str : Character.toLowerCase(str.charAt(0)) + str.substring(1);
  }

  public TypeName replaceTypeWith(TypeMirror typeMirror, TypeName replacement) {
    return switch (getContainerType(typeMirror)) {
      case NO_CONTAINER -> replacement;
      case LIST -> ParameterizedTypeName.get(ClassName.get(List.class), replacement);
      case RANGE -> ParameterizedTypeName.get(ClassName.get(Range.class), replacement);
      case MAP ->
          ParameterizedTypeName.get(
              ClassName.get(Map.class), TypeName.get(getMapKeyType(typeMirror)), replacement);
    };
  }

  public boolean isListType(TypeMirror type) {
    return isRawAssignable(type, List.class);
  }

  public boolean isRangeType(TypeMirror type) {
    return isRawAssignable(type, Range.class);
  }

  public boolean isMapType(TypeMirror type) {
    return isRawAssignable(type, Map.class);
  }

  public boolean isPrimitiveOrBoxed(TypeMirror type) {
    if (type.getKind().isPrimitive()) {
      return true;
    }
    return isSameRawType(type, Boolean.class)
        || isSameRawType(type, Byte.class)
        || isSameRawType(type, Character.class)
        || isSameRawType(type, Short.class)
        || isSameRawType(type, Integer.class)
        || isSameRawType(type, Long.class)
        || isSameRawType(type, Float.class)
        || isSameRawType(type, Double.class);
  }

  public boolean isString(TypeMirror type) {
    return isSameRawType(type, String.class);
  }

  public TypeMirror getMapValueType(TypeMirror typeMirror) {
    if (!isMapType(typeMirror)) {
      return typeMirror;
    }
    if (typeMirror instanceof DeclaredType declaredType
        && declaredType.getTypeArguments().size() >= 2) {
      return declaredType.getTypeArguments().get(1);
    }
    return objectType;
  }

  public TypeMirror getMapKeyType(TypeMirror typeMirror) {
    if (!isMapType(typeMirror)) {
      return typeMirror;
    }
    if (typeMirror instanceof DeclaredType declaredType
        && declaredType.getTypeArguments().size() >= 2) {
      return declaredType.getTypeArguments().get(0);
    }
    return objectType;
  }

  /**
   * Returns the package in which code should be generated for the given model root element. By
   * default, codegen happens in the same package as the modelRoot. But if the model root is shared,
   * i.e: @ModelRoot(isShared = true), then codegen happens in a sub-package
   */
  public String getCodegenPackageName(Element element) {
    ModelRoot modelRoot = element.getAnnotation(ModelRoot.class);
    if (modelRoot == null) {
      throw new IllegalArgumentException(
          "Cannot fetch codegen package name for Type which does not have @ModelRoot annotation");
    }
    String modelRootPackageName =
        requireNonNull(processingEnv().getElementUtils().getPackageOf(element))
            .getQualifiedName()
            .toString();
    if (modelRoot.isShared()) {
      return modelRootPackageName + "." + Constants.SHARED_MODELS_SUB_PACKAGE;
    }
    return modelRootPackageName;
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
                    .collect(joining(",\n")))
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

  public IfAbsent getIfAbsent(Element modelElement, @Nullable ModelRoot modelRoot) {
    TypeMirror returnType =
        modelElement instanceof ExecutableElement executableElement
            ? executableElement.getReturnType()
            : modelElement.asType();
    boolean optOrNullable = isOptional(returnType) || isAnyNullable(returnType, modelElement);
    // Check if the modelElement has the @IfAbsent annotation
    IfAbsent ifAbsent = modelElement.getAnnotation(IfAbsent.class);
    if (ifAbsent == null) {
      Set<ModelType> types = modelRoot == null ? Set.of() : Set.of(modelRoot.type());
      boolean isRequest = types.contains(ModelType.REQUEST);
      boolean isResponse = types.contains(ModelType.RESPONSE);
      if (isRequest && isResponse) {
        // For models with both REQUEST and RESPONSE, @IfAbsent is mandatory
        error(
            "Field '%s' in model with both REQUEST and RESPONSE types must have an explicit @IfAbsent annotation."
                .formatted(modelElement.getSimpleName()),
            modelElement);
        // Fallback to FAIL to continue processing
        ifAbsent = Creator.create(FAIL);
      } else if (isRequest) {
        ifAbsent = Creator.create(MAY_FAIL_CONDITIONALLY);
      } else if (optOrNullable) {
        ifAbsent = Creator.create(WILL_NEVER_FAIL);
      } else {
        ifAbsent = Creator.create(FAIL);
      }
    }
    return ifAbsent;
  }

  /**
   * Returns all model fields of the given ModelRoot element including the ones which have default
   * implementations and are not used for generating implementations
   */
  public ImmutableList<ExecutableElement> getModelFields(TypeElement modelRootElem) {
    if (modelRootElem.getAnnotation(ModelRoot.class) == null) {
      error("Model root type %s does not have @ModelRoot annotation", modelRootElem);
    }
    List<ExecutableElement> modelMethods = new ArrayList<>();

    for (ExecutableElement executableElement :
        ElementFilter.methodsIn(modelRootElem.getEnclosedElements())) {
      if (executableElement.getModifiers().contains(STATIC)) {
        // Static methods are considered application code which is ignored by Krystal
        continue;
      }
    }

    for (ExecutableElement method :
        ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(modelRootElem))) {
      if (isSameRawType(method.getEnclosingElement().asType(), Object.class)
          || isSameRawType(method.getEnclosingElement().asType(), Model.class)
          || isSameRawType(method.getEnclosingElement().asType(), ImmutableModel.class)
          || isSameRawType(method.getEnclosingElement().asType(), ImmutableModel.Builder.class)) {
        // Since these methods are defined in platform classes, ignore them.
        continue;
      }

      if (!method.getModifiers().contains(STATIC)) {
        if (method.getSimpleName().toString().startsWith("_")) {
          // Ignore methods starting with '_' as they are not model fields and are reserved for
          // platform use.
          continue;
        }
        validateModelRootMethod(method);
        modelMethods.add(method);
      }
    }
    return ImmutableList.copyOf(modelMethods);
  }

  /**
   * Extracts model methods from the model root interface which are eligible for code-generation
   * (i.e. these methods do not have a default implementation)
   *
   * @param modelRootElem The type element representing the model root
   * @return List of validated executable elements representing model methods
   */
  public ImmutableList<ExecutableElement> getModelFieldsForCodegen(TypeElement modelRootElem) {
    return getModelFields(modelRootElem).stream()
        .filter(method -> isMethodEligibleForModelCodeGen(method, modelRootElem))
        .collect(toImmutableList());
  }

  private void validateModelRootMethod(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    // Validate method return type is not an array
    if (returnType.getKind() == TypeKind.ARRAY) {
      error(
          "Model root methods must not return arrays. Use List or PrimitiveArray instead."
              + method.getSimpleName(),
          method);
    }

    if (method.getSimpleName().toString().startsWith("_")) {
      // Application code cannot define methods starting with '_'
      error(
          "Model field method names cannot start with an '_' as such names are considered reserved for platform use."
              + method.getSimpleName());
    }

    if (!method.getParameters().isEmpty()) {
      error("Model root methods must have zero parameters" + method.getSimpleName(), method);
    }

    if (returnType.getKind() == TypeKind.VOID) {
      error(
          "Model root methods must have a return type (not void)" + method.getSimpleName(), method);
    }

    // Validate no nested collections (List<List>, List<Map>, Map<K, List>, Map<K, Map>)
    validateNoNestedCollections(method, returnType);
  }

  public boolean isMethodEligibleForModelCodeGen(
      ExecutableElement method, TypeElement modelRootElem) {
    return
    // If the method is not a default method - then it is eligible for code generation
    !method.getModifiers().contains(DEFAULT)
        ||
        // If the method is a default method, but has @IfAbsent(ASSUME_DEFAULT_VALUE) -
        // then it is eligible for code generation
        ASSUME_DEFAULT_VALUE.equals(
            getIfAbsent(method, requireNonNull(modelRootElem.getAnnotation(ModelRoot.class)))
                .value());
  }

  private void validateNoNestedCollections(ExecutableElement method, TypeMirror type) {
    if (isOptional(type)) {
      type = getOptionalInnerType(type);
      if (isOptional(type)) {
        error(
            "Optional of Optional (Optional<Optional<..>>) is not supported by Krystal Modelling framework",
            method);
      }
    }
    if (isListType(type)) {
      TypeMirror elementType = getContentType(type);
      if (isListType(elementType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type List<List<...>>. Use a @ModelRoot model to wrap the inner List."
                    .formatted(method.getSimpleName()),
            method);
      }
      if (isMapType(elementType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type List<Map<...>>. Use a @ModelRoot model to wrap the inner Map."
                    .formatted(method.getSimpleName()),
            method);
      }
      if (isOptional(elementType)) {
        error(
            "Optional is not allowed as a List element type in Krystal models. "
                + "Field '%s' has type List<Optional<...>>.".formatted(method.getSimpleName()),
            method);
      }
    }
    if (isMapType(type)) {
      TypeMirror keyType = getMapKeyType(type);
      TypeMirror valueType = getMapValueType(type);
      if (isOptional(keyType)) {
        error(
            "Optional is not allowed as a Map key type in Krystal models. "
                + "Field '%s' has type Map<Optional<...>, ...>.".formatted(method.getSimpleName()),
            method);
      }
      if (isOptional(valueType)) {
        error(
            "Optional is not allowed as a Map value type in Krystal models. "
                + "Field '%s' has type Map<..., Optional<...>>.".formatted(method.getSimpleName()),
            method);
      }
      if (isMapType(keyType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type Map<Map<...>, ...>. Use a @ModelRoot model to wrap the inner Map."
                    .formatted(method.getSimpleName()),
            method);
      }
      if (isMapType(valueType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type Map<..., Map<...>>. Use a @ModelRoot model to wrap the inner Map."
                    .formatted(method.getSimpleName()),
            method);
      }
      if (isListType(keyType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type Map<List<...>, ...>. Use a @ModelRoot model to wrap the inner List."
                    .formatted(method.getSimpleName()),
            method);
      }
      if (isListType(valueType)) {
        error(
            "Nested collections are not allowed in Krystal models. "
                + "Field '%s' has type Map<..., List<...>>. Use a @ModelRoot model to wrap the inner List."
                    .formatted(method.getSimpleName()),
            method);
      }
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

  public TypeMirror getOptionalInnerType(TypeMirror typeMirror) {
    if (!isOptional(typeMirror)) {
      return typeMirror;
    }
    if (typeMirror instanceof DeclaredType declaredType
        && !declaredType.getTypeArguments().isEmpty()) {
      return declaredType.getTypeArguments().get(0);
    }
    return objectType;
  }

  public TypeMirror getContentType(TypeMirror typeMirror) {
    if (isListType(typeMirror) || isRangeType(typeMirror)) {
      if (typeMirror instanceof DeclaredType declaredType
          && !declaredType.getTypeArguments().isEmpty()) {
        return declaredType.getTypeArguments().get(0);
      }
      return objectType;
    } else if (isMapType(typeMirror)) {
      if (typeMirror instanceof DeclaredType declaredType
          && declaredType.getTypeArguments().size() == 2) {
        return declaredType.getTypeArguments().get(1);
      }
      return objectType;
    } else {
      return typeMirror;
    }
  }

  @Nullable String getDisallowedMessage(
      TypeMirror type, ImmutableMap<Class<?>, String> disallowedTypes) {
    return disallowedTypes.entrySet().stream()
        .<@Nullable String>map(
            e -> {
              if (isRawAssignable(type, e.getKey())) {
                return e.getValue();
              } else {
                return null;
              }
            })
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
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

    ByteArrayOutputStream formattedCodeOutput = new ByteArrayOutputStream();
    byte[] codeBytes = code.getBytes(UTF_8);
    int exitCode;

    try {
      exitCode =
          codeFormatter.run(
              new ByteArrayInputStream(codeBytes),
              formattedCodeOutput,
              System.err,
              // "-" arg means format input from input stream provided above. See
              // com.google.googlejavaformat.java.CommandLineOptions for reference
              "-");
    } catch (Throwable e) {
      log.warn("Encountered exception while formatting generated code.", e);
      exitCode = -1;
    }
    if (exitCode != 0) {
      note("Could not format code for class " + canonicalClassName, originatingElement);
      formattedCodeOutput = new ByteArrayOutputStream(codeBytes.length);
      formattedCodeOutput.writeBytes(codeBytes);
    }
    try {
      JavaFileObject requestFile =
          processingEnv
              .getFiler()
              .createSourceFile(
                  canonicalClassName,
                  Optional.ofNullable(originatingElement).stream().toArray(Element[]::new));
      note("Successfully created source file %s".formatted(canonicalClassName));
      try (PrintWriter out = new PrintWriter(requestFile.openWriter())) {
        out.print(formattedCodeOutput.toString(UTF_8));
      }
    } catch (Exception e) {
      error(
          "Error creating java file for className: %s. Error: %s".formatted(canonicalClassName, e),
          originatingElement);
    }
  }

  public TypeElement getTypeElemFromAnnotationMember(Supplier<Class<?>> supplier) {
    return requireNonNull(
        (TypeElement)
            processingEnv.getTypeUtils().asElement(getTypeFromAnnotationMember(supplier)));
  }

  public TypeMirror getTypeFromAnnotationMember(Supplier<Class<?>> supplier) {
    try {
      var clazz = supplier.get();
      return requireNonNull(elementUtils.getTypeElement(requireNonNull(clazz.getCanonicalName())))
          .asType();
    } catch (MirroredTypeException mte) {
      TypeMirror typeMirror = mte.getTypeMirror();
      if (typeMirror == null) {
        throw new AssertionError(
            "This is possible only if exception mte was serialized and deserialized, which is not the case");
      }
      return typeMirror;
    }
  }

  public List<? extends TypeMirror> getTypesFromAnnotationMember(Supplier<Class<?>[]> supplier) {
    try {
      return Arrays.stream(supplier.get())
          .map(Class::getCanonicalName)
          .map(Objects::requireNonNull)
          .map(elementUtils::getTypeElement)
          .map(Objects::requireNonNull)
          .map(TypeElement::asType)
          .toList();
    } catch (MirroredTypesException mte) {
      return mte.getTypeMirrors();
    }
  }

  public List<TypeElement> getTypeElemsFromAnnotationMember(Supplier<Class<?>[]> supplier) {
    return getTypesFromAnnotationMember(supplier).stream()
        .map(t -> requireNonNull((TypeElement) processingEnv.getTypeUtils().asElement(t)))
        .toList();
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
      note("TargetType: " + targetType);
      note("TargetType TypeArgs: " + targetType.getTypeArguments());
      return ImmutableList.copyOf(targetType.getTypeArguments());
    }
    return ImmutableList.of();
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

  public CodeValidationException errorAndThrow(
      String message, Throwable cause, @Nullable Element... elements) {
    _error(message + " Cause: " + cause.getMessage(), elements);
    return new CodeValidationException(message, cause);
  }

  public void error(String message, @Nullable Element... elements) {
    _error(message, elements);
  }

  public void error(Exception e, @Nullable Element... elements) {
    _error(String.valueOf(e), elements);
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
    /*
    stackTrace[0] = Thread.getStackTrace()
    stackTrace[1] = CodeGenUtility.getCallerInfo()
    stackTrace[2] = a private util method (_note/_error)
    stackTrace[3] = a public util method
    stackTrace[4 and above] = the actual callers
    */
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
    return TypeName.get(dataType.typeMirror(processingEnv));
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
    Types typeUtils = processingEnv().getTypeUtils();
    return typeUtils.isAssignable(
        typeUtils.erasure(from),
        typeUtils.erasure(
            getTypeElement(checkNotNull(to.getCanonicalName()), processingEnv()).asType()));
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

  public Builder classBuilder(String simpleName, String generatedForCanonicalName) {
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
  public Builder classBuilder(
      String simpleName,
      List<TypeVariableName> typeVariableNames,
      String generatedForCanonicalName) {
    Builder classBuilder;
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
    classBuilder.addAnnotation(Slf4j.class);
    return classBuilder;
  }

  public Builder interfaceBuilder(String interfaceName, String generatedForCanonicalName) {
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
  public Builder interfaceBuilder(
      String interfaceName,
      List<TypeVariableName> typeVariableNames,
      String generatedForCanonicalName) {
    Builder interfaceBuilder;
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
    return new TypeAndName(TypeName.get(boxed).annotated(javaType.typeName().annotations), boxed);
  }

  public TypeAndName getTypeName(CodeGenType dataType) {
    return new TypeAndName(dataType.typeMirror(processingEnv));
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
    TypeMirror typeMirror = javaType.typeMirror(processingEnv);
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
      AnnotationMirror annotationMirror, String annoElement, Class<T> type) {
    return type.cast(
        elementUtils.getElementValuesWithDefaults(annotationMirror).entrySet().stream()
            .filter(e -> e.getKey().getSimpleName().contentEquals(annoElement))
            .findAny()
            .map(Entry::getValue)
            .orElseThrow(AssertionError::new)
            .getValue());
  }

  public TypeName optional(TypeAndName javaType) {
    return ParameterizedTypeName.get(ClassName.get(Optional.class), box(javaType).typeName());
  }

  public ClassName getImmutInterfaceName(Element modelRootType) {
    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    if (modelRoot == null) {
      throw new IllegalArgumentException(
          "Cannot fetch Immut class name for Model which does not have @ModelRoot annotation");
    }

    String modelRootName = modelRootType.getSimpleName().toString();
    String packageName = getCodegenPackageName(modelRootType);
    return ClassName.get(packageName, modelRootName + modelRoot.suffixSeparator() + IMMUT_SUFFIX);
  }

  public TypeName getImmutTypeName(
      TypeMirror modelRootType, @Nullable ModelProtocol modelProtocol) {
    Optional<ModelRootInfo> modelRootInfo = asModelRoot(modelRootType);
    if (modelRootInfo.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot fetch Immut class name for class which does not have @ModelRoot annotation");
    }
    TypeMirror type = modelRootInfo.get().type();
    List<? extends TypeMirror> typeArguments = List.of();
    if (type instanceof DeclaredType declaredType) {
      typeArguments = declaredType.getTypeArguments();
    }
    ClassName immutClassName = getImmutClassName(modelRootInfo.get().element(), modelProtocol);
    return typeArguments.isEmpty()
        ? immutClassName
        : ParameterizedTypeName.get(
            immutClassName, typeArguments.stream().map(TypeName::get).toArray(TypeName[]::new));
  }

  public ClassName getImmutClassName(
      TypeElement modelRootElem, @Nullable ModelProtocol modelProtocol) {
    ClassName immutInterfaceName = getImmutInterfaceName(modelRootElem);
    if (modelProtocol == null) {
      return immutInterfaceName;
    }
    return ClassName.get(
        immutInterfaceName.packageName(),
        immutInterfaceName.simpleName() + modelProtocol.modelClassesSuffix());
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

  public boolean isPrimitiveArray(TypeMirror type) {
    return isRawAssignable(type, PrimitiveArray.class);
  }

  public record ModelFieldTypeInfo(
      TypeName fieldType, ContainerType containerType, TypeName elementType) {}

  /**
   * Returns the TypeName to be used as the type of the field in a model.
   *
   * @param method the method in the model root corresponding to the model field.
   * @param isBuilder true of field is in a builder class. false if it's in an immutable class.
   * @param modelProtocol the model protocol to be used for the field. null means don't use any
   *     specific model
   * @return
   */
  public ModelFieldTypeInfo getModelFieldType(
      ExecutableElement method, boolean isBuilder, @Nullable ModelProtocol modelProtocol) {
    final TypeMirror specifiedType = method.getReturnType();
    boolean isNullable = isAnyNullable(specifiedType, method);
    TypeMirror inferredType = specifiedType;
    boolean isOptional = isOptional(inferredType);
    if (isOptional) {
      // For Optional<T>, use T as the parameter type
      inferredType = getOptionalInnerType(inferredType);
    }
    if (isBuilder && inferredType instanceof PrimitiveType primitiveType) {
      inferredType = typeUtils.boxedClass(primitiveType).asType();
    }
    boolean isList = isListType(inferredType);
    boolean isMap = isMapType(inferredType);
    TypeMirror contentType;
    if (isList || isMap) {
      contentType = getContentType(inferredType);
    } else {
      contentType = inferredType;
    }
    TypeName typeName = contentType.accept(new TypeNameVisitor(), null);

    if (!isList && !isMap) {
      // Add @Nullable annotation for Optional types or methods with @Nullable annotation
      if (isOptional || isNullable || isBuilder) {
        if (!hasNullableAnnotation(typeName)) {
          // Add @Nullable as a type annotation
          typeName =
              typeName.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
        }
      }
    }
    TypeName finalTypeName;
    TypeName elementType = typeName;
    Optional<ModelRootInfo> fieldModelRootInfo = asModelRoot(inferredType, method);
    ContainerType containerType = getContainerType(inferredType);

    finalTypeName =
        switch (containerType) {
          case NO_CONTAINER -> {
            if (fieldModelRootInfo.isPresent()
                && !isEnumModel(fieldModelRootInfo.get().element())) {
              if (isBuilder) {
                if (!fieldModelRootInfo.get().annotation().builderExtendsModelRoot()) {
                  yield ClassName.get(Object.class);
                }
              } else {
                yield getImmutClassName(fieldModelRootInfo.get().element(), modelProtocol);
              }
            }
            yield typeName;
          }
          case RANGE -> typeName;
          case LIST -> {
            if (isBuilder) {
              if (fieldModelRootInfo.isPresent()
                  && !isEnumModel(fieldModelRootInfo.get().element())) {
                ClassName immutType =
                    getImmutClassName(fieldModelRootInfo.get().element(), modelProtocol);
                yield ParameterizedTypeName.get(
                    ClassName.get(ModelsListBuilder.class),
                    typeName,
                    immutType,
                    immutType.nestedClass("Builder"));
              } else {
                yield ParameterizedTypeName.get(
                    ClassName.get(List.class), TypeName.get(contentType));
              }
            } else {
              yield ParameterizedTypeName.get(
                  ClassName.get(ImmutableList.class), TypeName.get(contentType));
            }
          }
          case MAP -> {
            TypeName mapKeyTypeName =
                getMapKeyType(inferredType).accept(new TypeNameVisitor(), null);
            TypeName mapValueTypeName = typeName;
            if (isBuilder) {
              if (fieldModelRootInfo.isPresent()
                  && !isEnumModel(fieldModelRootInfo.get().element())) {
                ClassName immutType =
                    getImmutClassName(fieldModelRootInfo.get().element(), modelProtocol);
                yield ParameterizedTypeName.get(
                    ClassName.get(ModelsMapBuilder.class),
                    mapKeyTypeName,
                    mapValueTypeName,
                    immutType,
                    immutType.nestedClass("Builder"));
              } else {
                yield ParameterizedTypeName.get(
                    ClassName.get(Map.class), mapKeyTypeName, TypeName.get(contentType));
              }
            } else {
              yield ParameterizedTypeName.get(
                  ClassName.get(ImmutableMap.class), mapKeyTypeName, TypeName.get(contentType));
            }
          }
        };
    return new ModelFieldTypeInfo(finalTypeName, containerType, elementType);
  }

  /**
   * Determines the parameter type for a method return type, handling Optional types.
   *
   * @param method The model method for whose return type we need to compute the corresponding
   *     parameter type
   * @param isBuilder is for a parameter or field in a builder class? Or a parameter/field of the
   *     immut class?
   * @return The appropriate parameter type
   */
  public TypeName getVariableType(ExecutableElement method, boolean isBuilder) {
    final TypeMirror specifiedType = method.getReturnType();
    boolean isNullable = isAnyNullable(specifiedType, method);
    TypeMirror inferredType = specifiedType;
    boolean isOptional = isOptional(specifiedType);
    if (isOptional) {
      // For Optional<T>, use T as the parameter type
      inferredType = getOptionalInnerType(specifiedType);
    }
    if (isBuilder && inferredType instanceof PrimitiveType primitiveType) {
      inferredType = typeUtils.boxedClass(primitiveType).asType();
    }

    TypeName typeName;
    Optional<ModelRootInfo> modelRootInfo = asModelRoot(inferredType, method);
    if (modelRootInfo.isPresent()
        && modelRootInfo.get().containerType().isContainer()
        && !isEnumModel(modelRootInfo.get().element())) {
      typeName =
          replaceTypeWith(
              inferredType,
              WildcardTypeName.subtypeOf(
                  getContentType(inferredType).accept(new TypeNameVisitor(), null)));
    } else {
      typeName = inferredType.accept(new TypeNameVisitor(), null);
    }

    // Add @Nullable annotation for Optional types or methods with @Nullable annotation
    if (isOptional || isNullable || isBuilder) {
      if (!hasNullableAnnotation(typeName)) {
        // Add @Nullable as a type annotation
        typeName =
            typeName.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
      }
    }
    return typeName;
  }

  private static boolean hasNullableAnnotation(TypeName typeName) {
    AnnotationSpec nullableAnnoSpec = AnnotationSpec.builder(ClassName.get(Nullable.class)).build();
    boolean hasNullable = typeName.annotations.contains(nullableAnnoSpec);
    if (hasNullable) {
      return true;
    }
    if (typeName instanceof ParameterizedTypeName parameterizedTypeName) {
      return hasNullableAnnotation(parameterizedTypeName.rawType);
    }
    return false;
  }

  /**
   * Returns the list of TypeElements representing ModelProtocol classes from the repeatable
   * {@code @SupportedModelProtocol} annotations on the given element.
   */
  public List<TypeElement> getSupportedProtocolTypeElements(Element element) {
    SupportedModelProtocol[] protocols = element.getAnnotationsByType(SupportedModelProtocol.class);
    SupportedModelProtocolName[] protocolNames =
        element.getAnnotationsByType(SupportedModelProtocolName.class);
    return Stream.concat(
            stream(protocols).map(p -> getTypeElemFromAnnotationMember(p::value)),
            stream(protocolNames)
                .map(p -> elementUtils.getTypeElement(p.value()))
                .filter(Objects::nonNull)
                .map(Objects::requireNonNull))
        .toList();
  }

  /**
   * Returns the TypeElement of the default ModelProtocol from the repeatable
   * {@code @SupportedModelProtocol} annotations on the given element (the one with {@code isDefault
   * = true}). Returns {@code null} if no default is declared.
   */
  public @Nullable TypeElement getDefaultProtocolTypeElement(TypeMirror modelType) {
    AnnotationMirror defaultSerdeProtocolMirror =
        getAnnotationMirror(modelType, DefaultSerdeProtocol.class);
    if (defaultSerdeProtocolMirror != null) {
      return elementUtils.getTypeElement(
          (CharSequence)
              defaultSerdeProtocolMirror.getElementValues().entrySet().stream()
                  .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                  .findFirst()
                  .map(Entry::getValue)
                  .orElseThrow(AssertionError::new)
                  .getValue());
    }
    Element element = typeUtils.asElement(modelType);
    if (element == null) {
      return null;
    }
    DefaultSerdeProtocol defaultSerdeProtocol = element.getAnnotation(DefaultSerdeProtocol.class);
    DefaultSerdeProtocolName defaultSerdeProtocolName =
        element.getAnnotation(DefaultSerdeProtocolName.class);
    if (defaultSerdeProtocol != null && defaultSerdeProtocolName != null) {
      error(
          "Only one of @DefaultSerdeProtocol or @DefaultSerdeProtocolName should be specified - since a type can only have one default",
          element);
    }
    if (defaultSerdeProtocol != null) {
      return getTypeElemFromAnnotationMember(defaultSerdeProtocol::value);
    }
    if (defaultSerdeProtocolName != null) {
      TypeElement typeElement = elementUtils.getTypeElement(defaultSerdeProtocolName.value());
      if (typeElement == null) {
        note(
            "Ignoring %s as DefaultSerdeProtocol since loading type element for protocol returned null - maybe its not in the classpath"
                .formatted(defaultSerdeProtocolName.value()));
      } else {
        return typeElement;
      }
    }
    return null;
  }

  /** Returns true if the model root type supports the given model protocol. */
  public boolean typeExplicitlySupportsProtocol(
      Element modelRootType, Class<? extends ModelProtocol> modelProtocol) {
    return getSupportedProtocolTypeElements(modelRootType).stream()
        .map(element -> element.getQualifiedName().toString())
        .anyMatch(s -> Objects.equals(s, modelProtocol.getCanonicalName()));
  }

  /**
   * Returns the list of SerdeProtocol TypeMirrors from the @SupportedModelProtocol annotations on
   * the given element. Only protocols that extend SerdeProtocol are included.
   */
  public List<ModelProtocol> getModelProtocols(Element modelRootType) {
    List<TypeElement> supportedProtocolTypeElements =
        getSupportedProtocolTypeElements(modelRootType);
    if (supportedProtocolTypeElements.isEmpty()) {
      return List.of();
    }
    Map<String, ModelProtocol> availableModelProtocols =
        ServiceLoader.load(ModelProtocolConfigProvider.class, this.getClass().getClassLoader())
            .stream()
            .map(Provider::get)
            .map(ModelProtocolConfigProvider::getConfig)
            .map(ModelProtocolConfig::modelProtocol)
            .collect(
                toMap(c -> requireNonNull(c.getClass().getCanonicalName()), Function.identity()));

    return supportedProtocolTypeElements.stream()
        .map(element -> element.getQualifiedName().toString())
        .map(availableModelProtocols::get)
        .filter(Objects::nonNull)
        .map(Objects::requireNonNull)
        .toList();
  }

  /**
   * Returns true if the given element's @SupportedModelProtocol annotations contain the protocol
   * represented by the given TypeMirror.
   */
  public boolean typeSupportsProtocolByMirror(Element modelRootType, TypeMirror protocolMirror) {
    SupportedModelProtocol[] protocols =
        modelRootType.getAnnotationsByType(SupportedModelProtocol.class);
    if (protocols.length == 0) {
      return false;
    }
    return stream(protocols)
        .map(p -> getTypeElemFromAnnotationMember(p::value))
        .anyMatch(
            typeElement ->
                typeUtils.isSameType(typeElement.asType(), typeUtils.erasure(protocolMirror)));
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

  public static TypeName withTypeParams(
      ClassName className, List<? extends TypeParameterElement> typeParameterElements) {
    return asTypeNameWithTypes(
        className, typeParameterElements.stream().map(TypeParameterElement::asType).toList());
  }

  public static TypeName asTypeNameWithTypes(
      TypeName className, List<? extends TypeMirror> typeParams) {
    TypeNameVisitor typeNameVisitor = new TypeNameVisitor(true);
    List<? extends TypeName> typeNames = typeParams.stream().map(typeNameVisitor::visit).toList();
    return asTypeName(className, typeNames);
  }

  public static TypeName asTypeName(TypeName typeName, List<? extends TypeName> typeNames) {
    if (typeNames.isEmpty()) {
      return typeName;
    } else if (typeName instanceof ClassName className) {
      return ParameterizedTypeName.get(className, typeNames.toArray(TypeName[]::new));
    } else {
      throw new IllegalArgumentException(
          "If there are type names, the TypeName should be a ClassName");
    }
  }

  public <T extends Annotation> @Nullable AnnotationMirror getAnnotationMirror(
      AnnotatedConstruct annotatedConstruct, Class<T> annoClass) {
    return getAnnotationMirror(annotatedConstruct, requireNonNull(annoClass.getCanonicalName()));
  }

  public @Nullable AnnotationMirror getAnnotationMirror(
      AnnotatedConstruct annotatedConstruct, String annoClassCanonicalName) {
    Optional<? extends AnnotationMirror> any =
        annotatedConstruct.getAnnotationMirrors().stream()
            .filter(
                annotationMirror ->
                    annotationMirror.getAnnotationType().asElement() instanceof QualifiedNameable q
                        && q.getQualifiedName().contentEquals(annoClassCanonicalName))
            .findAny();
    return any.isPresent() ? any.get() : null;
  }

  public <T extends Annotation> @Nullable AnnotationInfo<T> getAnnotationInfo(
      Element annotatedElement, Class<T> annoClass) {
    T annotation = annotatedElement.getAnnotation(annoClass);
    Optional<? extends AnnotationMirror> mirror =
        annotatedElement.getAnnotationMirrors().stream()
            .filter(
                annotationMirror ->
                    annotationMirror.getAnnotationType().asElement() instanceof QualifiedNameable q
                        && q.getQualifiedName()
                            .contentEquals(requireNonNull(annoClass.getCanonicalName())))
            .findAny();
    if (annotation != null && mirror.isPresent()) {
      return new AnnotationInfo<>(annotation, mirror.get());
    }
    return null;
  }

  public <T extends Annotation> List<AnnotationInfo<T>> getAnnotationInfos(
      TypeMirror annotatedType,
      Class<T> annoClass,
      Function<Map<String, AnnotationValue>, T> annoCreator) {
    // annotatedType.getAnnotation(annoClass) always returns null - we need to use AnnotationMirror
    return getAnnotationInfos(annotatedType.getAnnotationMirrors(), annoClass, annoCreator);
  }

  public <T extends Annotation> List<AnnotationInfo<T>> getAnnotationInfos(
      List<? extends AnnotationMirror> annotationMirrors,
      Class<T> annoClass,
      Function<Map<String, AnnotationValue>, T> annoCreator) {
    return annotationMirrors.stream()
        .filter(
            annotationMirror ->
                annotationMirror.getAnnotationType().asElement() instanceof QualifiedNameable q
                    && q.getQualifiedName()
                        .contentEquals(requireNonNull(annoClass.getCanonicalName())))
        .map(mirror -> new AnnotationInfo<>(annotationFromMirror(mirror, annoCreator), mirror))
        .toList();
  }

  public <T extends Annotation> T annotationFromMirror(
      AnnotationMirror mirror, Function<Map<String, AnnotationValue>, T> annoCreator) {

    Map<String, AnnotationValue> elementValuesWithDefaults = new LinkedHashMap<>();
    elementUtils
        .getElementValuesWithDefaults(mirror)
        .forEach(
            (key, value) -> elementValuesWithDefaults.put(key.getSimpleName().toString(), value));

    return annoCreator.apply(elementValuesWithDefaults);
  }

  public boolean isModelRoot(TypeMirror javaModelType, Element... elements) {
    return asModelRoot(javaModelType, elements).isPresent();
  }

  public ContainerType getContainerType(TypeMirror javaModelType, Element... elements) {
    javaModelType = getOptionalInnerType(javaModelType);
    if (isOptional(javaModelType)) {
      error(
          "Optional of Optional (Optional<Optional<..>>) is not supported by Krystal Modelling framework",
          elements);
    }
    if (isListType(javaModelType)) {
      if (isListType(getContentType(javaModelType))) {
        error(
            "List of Lists (List<List<..>>) is not supported by Krystal Modelling protocol",
            elements);
      }
      return ContainerType.LIST;
    } else if (isMapType(javaModelType)) {
      if (isMapType(getContentType(javaModelType))) {
        error(
            "Map of Maps (Map<K, Map<..>>) is not supported by Krystal Modelling protocol",
            elements);
      }
      return ContainerType.MAP;
    } else if (isRangeType(javaModelType)) {
      return ContainerType.RANGE;
    }
    return ContainerType.NO_CONTAINER;
  }

  public Optional<ModelRootInfo> asModelRoot(TypeMirror javaModelType, Element... elements) {
    TypeMirror contentType = getContentType(getOptionalInnerType(javaModelType));
    if (!isRawAssignable(contentType, Model.class)) {
      return Optional.empty();
    }
    ContainerType containerType = getContainerType(javaModelType, elements);
    if (typeUtils.asElement(contentType) instanceof TypeElement typeElement) {
      ModelRoot annotation = typeElement.getAnnotation(ModelRoot.class);
      if (annotation != null) {
        return Optional.of(new ModelRootInfo(typeElement, contentType, annotation, containerType));
      }
    }
    return Optional.empty();
  }

  /** Returns true if the given TypeElement is an enum that implements {@link EnumModel}. */
  public boolean isEnumModel(@Nullable Element typeElement) {
    if (typeElement == null) {
      return false;
    }
    return typeElement.getKind() == ElementKind.ENUM;
  }

  /**
   * Returns true if the given TypeMirror is an enum type annotated with {@link ModelRoot} and
   * implementing {@link EnumModel}.
   */
  public boolean isEnumModelType(TypeMirror type) {
    Element element = processingEnv().getTypeUtils().asElement(type);
    if (element instanceof TypeElement typeElement) {
      return isEnumModel(typeElement);
    }
    return false;
  }

  public record ModelRootInfo(
      TypeElement element, TypeMirror type, ModelRoot annotation, ContainerType containerType) {}

  public record AnnotationInfo<T>(T annotation, AnnotationMirror mirror) {}

  public enum ContainerType {
    NO_CONTAINER,
    LIST,
    MAP,
    RANGE,
    ;

    public boolean isContainer() {
      return NO_CONTAINER != this;
    }
  }
}
