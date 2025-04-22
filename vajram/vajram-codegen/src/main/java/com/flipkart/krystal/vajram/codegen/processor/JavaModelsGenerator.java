package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.data.IfNull.IfNullThen.FAIL;
import static com.flipkart.krystal.data.IfNull.IfNullThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.data.IfNull.IfNullThen.WILL_NEVER_FAIL;
import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getIfNoValue;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

// Note: Using direct class references to model framework interfaces instead of imports
// since the actual package paths may differ across environments
import com.flipkart.krystal.data.IfNull;
import com.flipkart.krystal.data.IfNull.IfNullThen;
import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModelType;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelBuilder;
import com.flipkart.krystal.model.ModelBuilderType;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.ModelsCodeGenContext;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A ModelCodeGenerator generates sub interfaces and implementation classes for "ModelRoots". A
 * model root is an interface which extends the {@link Model} interface, and has the {@link
 * ModelRoot} annotation. Interfaces which extend a {@link Model} but do not have the {@link
 * ModelRoot} annotation are ignored by this class.
 *
 * <p>The generated subclasses and sub-interfaces only override or implement "model data accessor
 * methods". A method is not considered one if it is a default method whose names start with '_' -
 * these are considered "meta" methods which are not designed to be accessors for model data and are
 * ignored by this code generator..
 *
 * <p>This class throws an error if any of the following conditions are not satisfied:
 *
 * <ul>
 *   <li>The type with {@link ModelRoot} annotation is MUST be an interface
 *   <li>The interface with @{@link ModelRoot} annotation MUST extend {@link Model}
 *   <li>All model data accessor methods in the interface MUST have zero parameters
 *   <li>All model data accessor methods in the interface MUST have a return type. {@code void} and
 *       {@link Void} are not supported
 *   <li>None of the model data accessor method return types must be arrays
 * </ul>
 *
 * The generated subclasses and sub-interfaces do not override or implement default methods whose
 * names start with '_' - these are considered "meta" methods which are not designed to be accessors
 * for model data.
 *
 * <p>This code generator always generates the following classes:
 *
 * <ul>
 *   <li>Immutable Model interface which extends the Model Root and extends {@link ImmutableModel}.
 *       The name of this interface is the name of the Model Root suffixed with "_Immut". It has a
 *       default implementation of the {@link Model#_build()} method and returns {@code this}.
 *   <li>A Builder interface named "Builder" as an inner class of the Immutable Model mentioned
 *       above. This Builder interface only extends {@link ModelBuilder}.
 * </ul>
 *
 * In addition, if the Model Root doesn't have the {@link SupportedModelProtocols} annotation, or if
 * it does, and {@link SupportedModelProtocols#value()} contains {@link PlainJavaObject}, then this
 * generator also generates the following classes in the same package as the model root:
 *
 * <ul>
 *   <li>An Immutable model pojo final class which extends the above generated Immutable model
 *       interface. The class name is the model root name suffixed with "_ImmutPojo". This class has
 *       one field each corresponding each method in the Model root. All the methods defined in the
 *       model root are implemented in this class and return the corresponding field. The class has
 *       a package-private all argument constructor. It also has a public static "_builder" which
 *       return the Builder mentioned below. If any of the methods in the Model Root return an
 *       {@code Optional<T>}, then the corresponding field in this class also is an Optional, but
 *       the corresponding constructor param is a @{@link Nullable} T and is converted into Optional
 *       in the constructor. The class also implements {@link Model#_asBuilder()} method in which it
 *       calls the all arg constructor of the Builder mentioned below, and it implements {@link
 *       Model#_newCopy()} in which it calls its own all arg constructor.
 *   <li>A final class named "Builder" which is an inner class of the above generated Immutable
 *       model pojo class. This class extends the above generated Builder interface inside the
 *       generated Immutable Model interface. This class has setters corresponding to each method in
 *       the Model Root. All setters set the values to fields in the class. All fields are nullable
 *       and all setters accept nulls. This class has a package private no arg constructor, and a
 *       package private all args constructor. The class implements the {@link Model#_build()}
 *       method which calls the all arg constructor of the pojo class. The builder class respects
 *       the @ {@link IfNull} annotation and does necessary validations before calling the pojo
 *       constructor. implements {@link Model#_newCopy()} in which it creates a new Builder and sets
 *       all the values.
 * </ul>
 *
 * <p>Following are the recommendations for writing model roots (These are suggestions only and not
 * enforced)
 *
 * <ul>
 *   <li>It is RECOMMENDED that methods in the model root not have the "get" prefix
 *   <li>It is RECOMMENDED that methods returns types be Immutable
 * </ul>
 */
public final class JavaModelsGenerator implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final Utils util;

  public JavaModelsGenerator(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    util = codeGenContext.util();
  }

  /**
   * Generates model classes for the model root interface.
   *
   * <p>1. Validates that the model root type is an interface and extends Model
   *
   * <p>2. Generates an immutable interface with suffix _Immut
   *
   * <p>3. Generates a Builder interface inside the immutable interface
   *
   * <p>4. If PlainJavaObject is supported, generates a POJO implementation and a Builder
   * implementation
   */
  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    validate();

    TypeElement modelRootType = codeGenContext.modelRootType();
    Utils util = codeGenContext.util();

    // Validate the model root type
    validateModelRoot(modelRootType, util);

    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    // Get package and class names
    String packageName =
        util.processingEnv().getElementUtils().getPackageOf(modelRootType).toString();
    String modelRootName = modelRootType.getSimpleName().toString();
    String immutableModelName = modelRootName + modelRoot.suffixSeperator() + "Immut";
    String immutablePojoName = modelRootName + modelRoot.suffixSeperator() + "ImmutPojo";

    // Generate the immutable interface and its builder interface
    TypeSpec immutableInterface =
        generateImmutableInterface(modelRootType, modelMethods, immutableModelName);

    // Generate the POJO class if PlainJavaObject is supported
    if (isPlainJavaObjectSupported(modelRootType, util)) {
      TypeSpec immutablePojo =
          generateImmutablePojo(modelMethods, immutableModelName, immutablePojoName);
      writeJavaFile(packageName, immutablePojo, modelRootType, util);
    }

    // Write the immutable interface to a file
    writeJavaFile(packageName, immutableInterface, modelRootType, util);
  }

  private boolean isApplicable() {
    return CodegenPhase.MODELS.equals(codeGenContext.codegenPhase());
  }

  private void validate() {
    validateModelRoot(codeGenContext.modelRootType(), codeGenContext.util());
  }

  /**
   * Validates that the model root type is an interface and extends Model.
   *
   * @param modelRootType The type element representing the model root
   * @param util Utilities for code generation
   * @throws RuntimeException if validation fails
   */
  private void validateModelRoot(TypeElement modelRootType, Utils util) {
    if (!modelRootType.getKind().isInterface()) {
      util.error(
          "Type with @ModelRoot annotation must be an interface: "
              + modelRootType.getQualifiedName(),
          modelRootType);
    }

    if (!extendsModel(modelRootType, util)) {
      util.error(
          "Interface with @ModelRoot annotation must extend Model: "
              + modelRootType.getQualifiedName(),
          modelRootType);
    }
  }

  private static boolean extendsModel(TypeElement modelRootType, Utils util) {
    boolean extendsModel = false;
    for (TypeMirror superInterface : modelRootType.getInterfaces()) {
      TypeElement superElement =
          requireNonNull(
              (TypeElement) util.processingEnv().getTypeUtils().asElement(superInterface));
      if (superElement.getQualifiedName().contentEquals(Model.class.getCanonicalName())
          || extendsModel(superElement, util)) {
        extendsModel = true;
        break;
      }
    }
    return extendsModel;
  }

  /**
   * Generates the immutable interface that extends the model root and ImmutableModel.
   *
   * @param modelRootType The type element representing the model root
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name for the immutable interface
   * @return TypeSpec for the immutable interface
   */
  private TypeSpec generateImmutableInterface(
      TypeElement modelRootType, List<ExecutableElement> modelMethods, String immutableModelName) {

    TypeMirror immutableModelType =
        getAnnotationFromSuperInterfaces(modelRootType, ImmutableModelType.class)
            .flatMap(t -> util.getTypeFromAnnotationMember(t::value))
            .orElse(
                util.processingEnv()
                    .getElementUtils()
                    .getTypeElement(ImmutableModel.class.getCanonicalName())
                    .asType());

    TypeMirror modelBuilderType =
        getAnnotationFromSuperInterfaces(modelRootType, ModelBuilderType.class)
            .flatMap(t -> util.getTypeFromAnnotationMember(t::value))
            .orElse(
                util.processingEnv()
                    .getElementUtils()
                    .getTypeElement(ImmutableModel.class.getCanonicalName())
                    .asType());
    // Create the builder interface
    TypeSpec builderInterface =
        TypeSpec.interfaceBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(modelBuilderType)
            .addMethods(generateBuilderInterfaceMethods(modelMethods, immutableModelName))
            .build();

    // Create the immutable interface
    return TypeSpec.interfaceBuilder(immutableModelName)
        .addModifiers(PUBLIC)
        .addSuperinterface(ClassName.get(modelRootType))
        .addSuperinterface(immutableModelType)
        .addMethod(
            MethodSpec.methodBuilder("_build")
                .addModifiers(PUBLIC, DEFAULT)
                .addAnnotation(Override.class)
                .returns(ClassName.get("", immutableModelName))
                .addStatement("return this")
                .build())
        .addType(builderInterface)
        .build();
  }

  private <T extends Annotation> Optional<T> getAnnotationFromSuperInterfaces(
      TypeElement typeElement, Class<T> annotationType) {
    T annotation = typeElement.getAnnotation(annotationType);
    if (annotation == null) {
      List<T> list =
          typeElement.getInterfaces().stream()
              .map(t -> util.processingEnv().getTypeUtils().asElement(t))
              .filter(e -> e instanceof TypeElement)
              .map(e -> (TypeElement) e)
              .map(te -> getAnnotationFromSuperInterfaces(te, annotationType))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .toList();
      if (list.isEmpty()) {
        return Optional.empty();
      } else if (list.size() > 1) {
        util.error(
            "More than one super interface has @ImmutableModelType annotation. Exected zero or one",
            typeElement);
        return Optional.empty();
      } else {
        return Optional.ofNullable(list.get(0));
      }
    } else {
      return Optional.of(annotation);
    }
  }

  /**
   * Generates methods for the builder interface.
   *
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @return List of method specs for the builder interface
   */
  private List<MethodSpec> generateBuilderInterfaceMethods(
      List<ExecutableElement> modelMethods, String immutableModelName) {
    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      // Validate optional fields
      validateOptionalField(method);
      String methodName = method.getSimpleName().toString();

      TypeMirror returnType = method.getReturnType();
      methods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC, ABSTRACT)
              .addParameter(getParameterType(returnType), methodName)
              .returns(ClassName.get("", "Builder"))
              .build());
    }

    methods.add(
        MethodSpec.methodBuilder("_build")
            .addModifiers(PUBLIC, ABSTRACT)
            .returns(ClassName.get("", immutableModelName))
            .build());

    return methods;
  }

  /**
   * Generates the immutable POJO class that implements the immutable interface.
   *
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name for the immutable POJO
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateImmutablePojo(
      List<ExecutableElement> modelMethods, String immutableModelName, String immutablePojoName) {

    // Create fields for the POJO class
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeName fieldType;

      // If the return type is Optional<T>, use T as the field type instead of Optional<T>
      if (util.isOptional(method.getReturnType())) {
        TypeMirror innerType = util.getOptionalInnerType(method.getReturnType());
        // Box primitive types
        if (innerType.getKind().isPrimitive()) {
          fieldType = TypeName.get(innerType).box();
        } else {
          fieldType = TypeName.get(innerType);
        }
      } else {
        // Check if the method has @IfNoValue and is primitive
        IfNull ifNull = getIfNoValue(method);
        if (method.getReturnType().getKind().isPrimitive()
            && (ifNull.value() == MAY_FAIL_CONDITIONALLY || ifNull.value() == WILL_NEVER_FAIL)) {
          fieldType = TypeName.get(method.getReturnType()).box();
        } else {
          fieldType = TypeName.get(method.getReturnType());
        }
      }

      // Add @Nullable annotation for Optional types or methods with @Nullable annotation
      if (util.isOptional(method.getReturnType()) || util.isNullable(method.getReturnType())) {
        // Add @Nullable as a type annotation
        fieldType =
            fieldType.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
      }
      fields.add(FieldSpec.builder(fieldType, fieldName, PRIVATE, FINAL).build());
    }

    // Create constructor for the POJO class
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(PRIVATE);

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      constructorBuilder.addParameter(
          ParameterSpec.builder(getParameterType(method.getReturnType()), fieldName).build());

      // For all field types, just assign the parameter directly
      constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
    }

    // Create getter methods for the POJO class
    List<MethodSpec> methods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      TypeName returnType = TypeName.get(method.getReturnType());

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addAnnotation(Override.class)
              .returns(returnType);

      // If the return type is Optional<T>, wrap the field in Optional.ofNullable()
      if (util.isOptional(method.getReturnType())) {
        methodBuilder.addStatement("return $T.ofNullable($N)", Optional.class, methodName);
      } else {
        methodBuilder.addStatement("return $N", methodName);
      }

      methods.add(methodBuilder.build());
    }

    // Create _asBuilder method to return a new Builder instance with all fields
    MethodSpec.Builder asBuilderMethodBuilder =
        MethodSpec.methodBuilder("_asBuilder")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(ClassName.get("", immutablePojoName + ".Builder"));

    // Initialize code to create a new Builder and set all fields
    asBuilderMethodBuilder.addCode(
        "return new $T()", ClassName.get("", immutablePojoName + ".Builder"));
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      asBuilderMethodBuilder.addCode(".$L($L)", fieldName, fieldName);
    }
    asBuilderMethodBuilder.addCode(";");

    // Create _newCopy method to return a new instance with the same values
    MethodSpec.Builder newCopyMethodBuilder =
        MethodSpec.methodBuilder("_newCopy")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(ClassName.get("", immutablePojoName));

    // Get list of field names for constructor arguments
    String fieldNames =
        modelMethods.stream()
            .map(m -> m.getSimpleName().toString())
            .collect(java.util.stream.Collectors.joining(", "));

    // Create a new instance of the POJO with current field values
    newCopyMethodBuilder.addStatement(
        "return new $T($L)", ClassName.get("", immutablePojoName), fieldNames);

    // Add methods to the list
    methods.add(asBuilderMethodBuilder.build());
    methods.add(newCopyMethodBuilder.build());

    // Create _builder static method
    MethodSpec builderMethod =
        MethodSpec.methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(ClassName.get("", immutablePojoName + ".Builder"))
            .addStatement("return new $T()", ClassName.get("", immutablePojoName + ".Builder"))
            .build();

    // Create builder class
    TypeSpec builderClass =
        generateBuilderClass(modelMethods, immutableModelName, immutablePojoName);

    // Create the POJO class
    return TypeSpec.classBuilder(immutablePojoName)
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(ClassName.get("", immutableModelName))
        .addFields(fields)
        .addMethod(constructorBuilder.build())
        .addMethods(methods)
        .addMethod(builderMethod)
        .addType(builderClass)
        .build();
  }

  /**
   * Generates the builder class for the immutable POJO.
   *
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name of the immutable POJO
   * @return TypeSpec for the builder class
   */
  private TypeSpec generateBuilderClass(
      List<ExecutableElement> modelMethods, String immutableModelName, String immutablePojoName) {

    // Create fields for the builder class
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeName fieldType;

      // If the return type is Optional<T>, use T as the field type
      if (util.isOptional(method.getReturnType())) {
        TypeMirror innerType = util.getOptionalInnerType(method.getReturnType());
        // Box primitive types
        if (innerType.getKind().isPrimitive()) {
          fieldType = TypeName.get(innerType).box();
        } else {
          fieldType = TypeName.get(innerType);
        }
      } else {
        // Box primitive types for methods with @IfNoValue(then = FAIL) or with platform defaults
        if (method.getReturnType().getKind().isPrimitive()) {
          // Box primitive types for methods with @IfNoValue(then = FAIL) or platform defaults
          fieldType = TypeName.get(method.getReturnType()).box();
        } else {
          fieldType = TypeName.get(method.getReturnType());
        }
      }

      // Add @Nullable annotation for Optional types or methods with @Nullable annotation
      if (util.isOptional(method.getReturnType()) || util.isNullable(method.getReturnType())) {
        // Add @Nullable as a type annotation
        TypeName annotatedType =
            fieldType.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
        fields.add(FieldSpec.builder(annotatedType, fieldName, PRIVATE).build());
      } else {
        fields.add(FieldSpec.builder(fieldType, fieldName, PRIVATE).build());
      }
    }

    // Create no-arg constructor
    MethodSpec noArgConstructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC).build();

    // Create setter methods
    List<MethodSpec> setterMethods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      setterMethods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addParameter(getParameterType(method.getReturnType()), methodName)
              .returns(ClassName.get("", "Builder"))
              .addStatement("this.$N = $N", methodName, methodName)
              .addStatement("return this")
              .addAnnotation(Override.class)
              .build());
    }

    // Create _build method
    MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("_build")
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", immutablePojoName))
            .addAnnotation(Override.class);

    // Validate fields based on IfNoValue annotation strategies
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();
      String typeName = returnType.toString();
      boolean isPrimitive = returnType.getKind().isPrimitive();

      // Default values for different types
      String defaultValue = "null";
      if (typeName.equals("boolean") || typeName.equals("java.lang.Boolean")) {
        defaultValue = "false";
      } else if (typeName.equals("byte")
          || typeName.equals("java.lang.Byte")
          || typeName.equals("short")
          || typeName.equals("java.lang.Short")
          || typeName.equals("int")
          || typeName.equals("java.lang.Integer")
          || typeName.equals("long")
          || typeName.equals("java.lang.Long")
          || typeName.equals("float")
          || typeName.equals("java.lang.Float")
          || typeName.equals("double")
          || typeName.equals("java.lang.Double")) {
        defaultValue = "0";
      } else if (typeName.equals("java.lang.String")) {
        defaultValue = "\"\"";
      } else if (typeName.contains("java.util.List")
          || typeName.contains("java.util.Collection")
          || typeName.contains("java.util.Set")) {
        defaultValue = "java.util.Collections.emptyList()";
      } else if (typeName.contains("java.util.Map")) {
        defaultValue = "java.util.Collections.emptyMap()";
      } else if (typeName.contains("[]")) {
        // Array type
        String componentType = typeName.substring(0, typeName.indexOf('['));
        defaultValue = "new " + componentType + "[0]";
      }

      // Get the IfNoValue annotation directly
      IfNull ifNull = getIfNoValue(method);

      // If IfNoValue annotation was found, handle according to strategy
      // Get the strategy directly from the annotation
      IfNullThen ifNullThen = ifNull.value();

      // Only generate null check if validation is needed or error needs to be thrown
      if (ifNullThen != IfNullThen.MAY_FAIL_CONDITIONALLY
          && ifNullThen != IfNullThen.WILL_NEVER_FAIL) {
        buildMethodBuilder.beginControlFlow("if (this.$N == null)", fieldName);

        switch (ifNullThen) {
          case FAIL:
            // FAIL strategy - throw exception when value is null
            buildMethodBuilder.addStatement(
                "throw new $T($S)",
                IllegalStateException.class,
                "Field '" + fieldName + "' is mandatory but no value was provided");
            break;
          case DEFAULT_TO_FALSE:
            // DEFAULT_TO_FALSE strategy - use false for boolean types
            buildMethodBuilder.addStatement("this.$N = false", fieldName);
            break;
          case DEFAULT_TO_ZERO:
            // DEFAULT_TO_ZERO strategy - use 0 for numeric types
            buildMethodBuilder.addStatement("this.$N = 0", fieldName);
            break;
          case DEFAULT_TO_EMPTY:
            // DEFAULT_TO_EMPTY strategy - use empty collection/map/string
            buildMethodBuilder.addStatement("this.$N = $L", fieldName, defaultValue);
            break;
          case DEFAULT_TO_MODEL_DEFAULTS:
            // DEFAULT_TO_MODEL_DEFAULTS strategy - create a new instance with defaults
            if (isPrimitive) {
              buildMethodBuilder.addStatement("this.$N = $L", fieldName, defaultValue);
            } else {
              // For Model types, try to create a new instance using builder pattern if available
              buildMethodBuilder.addStatement(
                  "// For Model types, ideally we would create a default instance here");
              buildMethodBuilder.addStatement("this.$N = null", fieldName);
            }
            break;
          default:
            throw new AssertionError("Unexpected IfNoValueThen = " + ifNullThen);
        }

        buildMethodBuilder.endControlFlow();
      } else {
        // MAY_FAIL_CONDITIONALLY strategy - this would require more complex logic
        // Since conditional logic would be defined elsewhere, we don't generate null check
        buildMethodBuilder.addComment(
            "MAY_FAIL_CONDITIONALLY strategy detected - conditional logic should be handled separately");
        buildMethodBuilder.addStatement("// No default handling for conditional strategy");
      }
    }

    // Build the POJO with all fields
    buildMethodBuilder.addCode("return new $T(", ClassName.get("", immutablePojoName));
    for (int i = 0; i < modelMethods.size(); i++) {
      String fieldName = modelMethods.get(i).getSimpleName().toString();
      buildMethodBuilder.addCode("$N", fieldName);
      if (i < modelMethods.size() - 1) {
        buildMethodBuilder.addCode(", ");
      }
    }
    buildMethodBuilder.addCode(");");

    // Create _newCopy method for the Builder
    MethodSpec.Builder builderCopyMethodBuilder =
        MethodSpec.methodBuilder("_newCopy")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(ClassName.get("", immutablePojoName + ".Builder"));

    // Initialize code to create a new Builder and set all fields
    builderCopyMethodBuilder.addCode(
        "return new $T()", ClassName.get("", immutablePojoName + ".Builder"));
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      builderCopyMethodBuilder.addCode(".$L(this.$L)", fieldName, fieldName);
    }
    builderCopyMethodBuilder.addCode(";");

    // Create the builder class
    return TypeSpec.classBuilder("Builder")
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(ClassName.get("", immutableModelName + ".Builder"))
        .addFields(fields)
        .addMethod(noArgConstructor)
        .addMethods(setterMethods)
        .addMethod(buildMethodBuilder.build())
        .addMethod(builderCopyMethodBuilder.build())
        .build();
  }

  /**
   * Checks if the model root supports PlainJavaObject serialization.
   *
   * @param modelRootType The type element representing the model root
   * @param util Utilities for code generation
   * @return True if PlainJavaObject is supported, false otherwise
   */
  private boolean isPlainJavaObjectSupported(TypeElement modelRootType, Utils util) {
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return true;
    }

    List<? extends TypeMirror> modelProtocols =
        util.getTypesFromAnnotationMember(supportedModelProtocols::value);
    if (modelProtocols.isEmpty()) {
      return true;
    }

    // Check if PlainJavaObject is mentioned in the annotation value
    return modelProtocols.stream()
        .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
        .filter(elem -> elem instanceof QualifiedNameable)
        .map(element -> requireNonNull((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(PlainJavaObject.class.getCanonicalName()::equals);
  }

  /**
   * Determines the parameter type for a method return type, handling Optional types.
   *
   * @param specifiedType The return type of the method
   * @return The appropriate parameter type
   */
  private TypeName getParameterType(TypeMirror specifiedType) {
    TypeMirror inferredType = specifiedType;
    if (util.isOptional(specifiedType)) {
      // For Optional<T>, use T as the parameter type
      inferredType = util.getOptionalInnerType(specifiedType);
    }
    TypeName typeName = TypeName.get(inferredType);
    if (typeName.isPrimitive()) {
      typeName = typeName.box();
    }
    // Add @Nullable annotation for Optional types or methods with @Nullable annotation
    if (util.isOptional(specifiedType) || util.isNullable(specifiedType)) {
      // Add @Nullable as a type annotation
      typeName = typeName.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
    }
    return typeName;
  }

  /**
   * Validates that conditionally optional fields are not primitive types and are properly
   * annotated. Fields with @IfNoValue(then=MAY_FAIL_CONDITIONALLY) must not be primitive types and
   * must be either Optional or annotated with @Nullable.
   */
  private void validateOptionalField(ExecutableElement method) {
    IfNull ifNull = getIfNoValue(method);
    if (ifNull.value() == MAY_FAIL_CONDITIONALLY) {
      // Check if the return type is primitive
      TypeMirror returnType = method.getReturnType();
      if (returnType.getKind().isPrimitive()) {
        util.error(
            "An optional field cannot be a primitive type. Use Optional<> or @Nullable boxed type instead",
            method);
      }

      // Check if the return type has @Nullable or is an Optional
      if (!util.isOptional(returnType) && !util.isNullable(returnType)) {
        util.error(
            "Field '%s' with @IfNoValue(then=MAY_FAIL_CONDITIONALLY) must be an Optional or annotated with %s: "
                .formatted(method.getSimpleName(), Nullable.class.getCanonicalName()),
            method);
      }
    } else if (ifNull.value() == FAIL) {
      // For FAIL strategy, we don't need to validate anything specific here
      // The code generation will handle boxing primitive types for these fields
    }
  }

  /**
   * Writes a Java file to the source directory.
   *
   * @param packageName The package name for the file
   * @param typeSpec The type specification to write
   * @param originatingElement The element that caused this file to be generated
   * @param util Utilities for code generation
   */
  private void writeJavaFile(
      String packageName, TypeSpec typeSpec, TypeElement originatingElement, Utils util) {
    try {
      JavaFile javaFile = JavaFile.builder(packageName, typeSpec).build();
      String fileName = packageName + "." + typeSpec.name;
      util.generateSourceFile(fileName, javaFile.toString(), originatingElement);
    } catch (Exception e) {
      util.error("Error generating Java file: " + e.getMessage(), originatingElement);
    }
  }
}
