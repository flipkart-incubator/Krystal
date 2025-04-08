package com.flipkart.krystal.vajram.codegen.processor;

import static com.flipkart.krystal.vajram.codegen.processor.VajramCodeGenerator.validateIfNoValueStrategyApplicability;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

// Note: Using direct class references to model framework interfaces instead of imports
// since the actual package paths may differ across environments
import com.flipkart.krystal.data.IfNoValue;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelBuilder;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.vajram.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.vajram.codegen.common.models.Utils;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.codegen.common.spi.ModelsCodeGenContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A ModelCodeGenerator generates sub interfaces and implementation classes for "ModelRoots". A
 * model root is an interface which extends the {@link Model} interface, and has the {@link
 * ModelRoot} annotation. Interfaces which extend a {@link Model} but do not have the {@link
 * ModelRoot} annotation are ignored by this class.
 *
 * <p>This class throws an error if any of the following conditions are not satisfied:
 *
 * <ul>
 *   <li>The type with {@link ModelRoot} annotation is MUST be an interface
 *   <li>The interface with @{@link ModelRoot} annotation MUST extend {@link Model}
 *   <li>All methods in the interface MUST have zero parameters
 *   <li>All methods in the interface MUST have a return type. {@code void} and {@link Void} are not
 *       supported
 *   <li>None of the method return types must be arrays
 * </ul>
 *
 * <p>This code generator always generates the following classes in the same package as the model
 * root:
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
 *       the @ {@link IfNoValue} annotation and does necessary validations before calling the pojo
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

  public JavaModelsGenerator(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
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

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = extractAndValidateModelMethods(modelRootType, util);

    // Get package and class names
    String packageName =
        util.processingEnv().getElementUtils().getPackageOf(modelRootType).toString();
    String modelRootName = modelRootType.getSimpleName().toString();
    String immutableModelName = modelRootName + "_Immut";
    String immutablePojoName = modelRootName + "_ImmutPojo";

    // Generate the immutable interface and its builder interface
    TypeSpec immutableInterface =
        generateImmutableInterface(modelRootType, modelMethods, immutableModelName);

    // Generate the POJO class if PlainJavaObject is supported
    if (isPlainJavaObjectSupported(modelRootType, util)) {
      TypeSpec immutablePojo =
          generateImmutablePojo(
              modelRootType, modelMethods, immutableModelName, immutablePojoName, util);
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
      throw util.errorAndThrow(
          "Type with @ModelRoot annotation must be an interface: "
              + modelRootType.getQualifiedName(),
          modelRootType);
    }

    boolean extendsModel = false;
    for (TypeMirror superInterface : modelRootType.getInterfaces()) {
      TypeElement superElement =
          (TypeElement) util.processingEnv().getTypeUtils().asElement(superInterface);
      if (superElement.getQualifiedName().contentEquals(Model.class.getCanonicalName())) {
        extendsModel = true;
        break;
      }
    }

    if (!extendsModel) {
      throw util.errorAndThrow(
          "Interface with @ModelRoot annotation must extend Model: "
              + modelRootType.getQualifiedName(),
          modelRootType);
    }
  }

  /**
   * Extracts and validates model methods from the model root interface.
   *
   * @param modelRootType The type element representing the model root
   * @param util Utilities for code generation
   * @return List of validated executable elements representing model methods
   */
  private List<ExecutableElement> extractAndValidateModelMethods(
      TypeElement modelRootType, Utils util) {
    List<ExecutableElement> modelMethods = new ArrayList<>();

    for (Element element : modelRootType.getEnclosedElements()) {
      if (ElementKind.METHOD.equals(element.getKind())) {
        ExecutableElement method = (ExecutableElement) element;

        validateGetterMethod(util, method);

        modelMethods.add(method);
      }
    }

    return modelMethods;
  }

  private static void validateGetterMethod(Utils util, ExecutableElement method) {
    // Validate method has zero parameters
    if (!method.getParameters().isEmpty()) {
      throw util.errorAndThrow(
          "Model root methods must have zero parameters: " + method.getSimpleName(), method);
    }

    TypeMirror returnType = method.getReturnType();

    // Validate method has a return type (not void)
    if (returnType.getKind() == TypeKind.VOID) {
      throw util.errorAndThrow(
          "Model root methods must have a return type (not void): " + method.getSimpleName(),
          method);
    }

    // Validate method return type is not an array
    if (returnType.getKind() == TypeKind.ARRAY) {
      throw util.errorAndThrow(
          "Model root methods must not return arrays. Use List instead.", method);
    }

    DataType<Object> dataType = new DeclaredTypeVisitor<>(util, method).visit(returnType);

    validateIfNoValueStrategyApplicability(method, dataType, util);
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

    // Create the builder interface
    TypeSpec builderInterface =
        TypeSpec.interfaceBuilder("Builder")
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(ClassName.get(ModelBuilder.class))
            .addMethods(generateBuilderInterfaceMethods(modelMethods, immutableModelName))
            .build();

    // Create the immutable interface
    return TypeSpec.interfaceBuilder(immutableModelName)
        .addModifiers(PUBLIC)
        .addSuperinterface(ClassName.get(modelRootType))
        .addSuperinterface(ClassName.get(ImmutableModel.class))
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
      String methodName = method.getSimpleName().toString();
      TypeName returnType = TypeName.get(method.getReturnType());

      methods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC, ABSTRACT)
              .addParameter(returnType, methodName)
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
   * @param modelRootType The type element representing the model root
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name for the immutable POJO
   * @param util Utilities for code generation
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateImmutablePojo(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      String immutableModelName,
      String immutablePojoName,
      Utils util) {

    // Create fields for the POJO class
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeName fieldType = getFieldType(method.getReturnType());

      fields.add(FieldSpec.builder(fieldType, fieldName, PRIVATE, FINAL).build());
    }

    // Create constructor for the POJO class
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(PRIVATE);

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeName paramType = getParameterType(method.getReturnType());
      TypeName fieldType = getFieldType(method.getReturnType());

      ParameterSpec.Builder paramBuilder = ParameterSpec.builder(paramType, fieldName);
      if (isOptionalReturnType(method.getReturnType())) {
        paramBuilder.addAnnotation(Nullable.class);
      }

      constructorBuilder.addParameter(paramBuilder.build());

      if (isOptionalReturnType(method.getReturnType())) {
        constructorBuilder.addStatement(
            "this.$N = $T.ofNullable($N)", fieldName, Optional.class, fieldName);
      } else {
        constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
      }
    }

    // Create getter methods for the POJO class
    List<MethodSpec> methods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      TypeName returnType = TypeName.get(method.getReturnType());

      methods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addAnnotation(Override.class)
              .returns(returnType)
              .addStatement("return $N", methodName)
              .build());
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
      TypeName fieldType = getParameterType(method.getReturnType());

      fields.add(FieldSpec.builder(fieldType, fieldName, PRIVATE).build());
    }

    // Create no-arg constructor
    MethodSpec noArgConstructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC).build();

    // Create setter methods
    List<MethodSpec> setterMethods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      TypeName paramType = getParameterType(method.getReturnType());

      setterMethods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addParameter(paramType, methodName)
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
            .returns(ClassName.get("", immutableModelName))
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
      IfNoValue ifNoValue = method.getAnnotation(IfNoValue.class);

      // If IfNoValue annotation was found, handle according to strategy
      if (ifNoValue != null) {
        // Get the strategy directly from the annotation
        IfNoValue.Strategy strategy = ifNoValue.then();
        buildMethodBuilder.beginControlFlow("if (this.$N == null)", fieldName);

        switch (strategy) {
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
          case MAY_FAIL_CONDITIONALLY:
            // MAY_FAIL_CONDITIONALLY strategy - this would require more complex logic
            // Since conditional logic would be defined elsewhere, we default to null here
            buildMethodBuilder.addComment(
                "MAY_FAIL_CONDITIONALLY strategy detected - conditional logic should be handled separately");
            buildMethodBuilder.addStatement("// No default handling for conditional strategy");
        }

        buildMethodBuilder.endControlFlow();
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
        .map(element -> ((QualifiedNameable) element).getQualifiedName().toString())
        .anyMatch(PlainJavaObject.class.getCanonicalName()::equals);
  }

  /**
   * Determines the field type for a method return type, handling Optional types.
   *
   * @param returnType The return type of the method
   * @return The appropriate field type
   */
  private TypeName getFieldType(TypeMirror returnType) {
    if (isOptionalReturnType(returnType)) {
      return TypeName.get(returnType); // Keep Optional as is for fields
    }
    return TypeName.get(returnType);
  }

  /**
   * Determines the parameter type for a method return type, handling Optional types.
   *
   * @param returnType The return type of the method
   * @return The appropriate parameter type
   */
  private TypeName getParameterType(TypeMirror returnType) {
    if (isOptionalReturnType(returnType)) {
      // For Optional<T>, use T as the parameter type
      DeclaredType declaredType = (DeclaredType) returnType;
      return TypeName.get(declaredType.getTypeArguments().get(0));
    }
    return TypeName.get(returnType);
  }

  /**
   * Checks if a type is an Optional.
   *
   * @param type The type to check
   * @return True if the type is an Optional, false otherwise
   */
  private boolean isOptionalReturnType(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    DeclaredType declaredType = (DeclaredType) type;
    Element element = declaredType.asElement();
    return element.toString().equals(Optional.class.getName());
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
