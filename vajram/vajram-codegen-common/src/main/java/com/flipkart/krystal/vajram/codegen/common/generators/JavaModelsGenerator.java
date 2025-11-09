package com.flipkart.krystal.vajram.codegen.common.generators;

import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.model.PlainJavaObject.POJO;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.DEFAULT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.MandatoryFieldMissingException;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.Trait;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
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
 * ignored by this code generator.
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
 *       above. This Builder interface only extends {@link Builder}.
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
 *       the @ {@link IfAbsent} annotation and does necessary validations before calling the pojo
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
  private final CodeGenUtility util;

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
    CodeGenUtility util = codeGenContext.util();

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    // Get package and class names
    ClassName immutModelName = util.getImmutClassName(modelRootType);
    String immutablePojoName = immutModelName.simpleName() + POJO.modelClassesSuffix();
    String packageName = immutModelName.packageName();

    // Generate the immutable interface and its builder interface
    TypeSpec immutableInterface =
        generateImmutableInterface(modelRootType, modelMethods, immutModelName);
    // Write the immutable interface to a file
    util.writeJavaFile(packageName, immutableInterface, modelRootType);

    if (codeGenContext.modelRootType().getAnnotation(SupportedModelProtocols.class) == null
        || util.typeExplicitlySupportsProtocol(modelRootType, PlainJavaObject.class)) {
      // Generate the POJO class if PlainJavaObject is supported
      TypeSpec immutablePojo =
          generateImmutablePojo(modelRootType, modelMethods, immutModelName, immutablePojoName);
      util.writeJavaFile(packageName, immutablePojo, modelRootType);
    }
  }

  private boolean isApplicable() {
    return MODELS.equals(codeGenContext.codegenPhase());
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
  private void validateModelRoot(TypeElement modelRootType, CodeGenUtility util) {
    if (!modelRootType.getKind().isInterface()) {
      util.error(
          "Type with @ModelRoot annotation must be an interface: "
              + modelRootType.getQualifiedName(),
          modelRootType);
    }

    checkArgument(
        modelRootType.getTypeParameters().isEmpty(),
        "Generic model roots are not currently supported.");

    if (!extendsModel(modelRootType, util)) {
      util.error(
          "Interface with @ModelRoot annotation must extend " + Model.class.getCanonicalName(),
          modelRootType);
    }
  }

  private static boolean extendsModel(TypeElement modelRootType, CodeGenUtility util) {
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
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName) {

    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    Optional<TypeElement> parentModelRootOpt =
        getParentInterfaceWithAnnotation(modelRootType, ModelRoot.class);
    boolean hasParentModelRoot = parentModelRootOpt.isPresent();
    Optional<TypeElement> modelClusterRoot =
        parentModelRootOpt.isPresent()
            ? Optional.empty()
            : getInterfaceWithAnnotation(modelRootType, ModelClusterRoot.class);
    ImmutableList<TypeMirror> typeParamTypes =
        modelClusterRoot
            .map(mcr -> util.getTypeParamTypes(modelRootType, mcr))
            .orElse(ImmutableList.of());

    Optional<ModelClusterRoot> modelClusterRootAnno =
        modelClusterRoot.map(typeElement -> typeElement.getAnnotation(ModelClusterRoot.class));

    Optional<? extends AnnotationMirror> parentModelRootAnno =
        parentModelRootOpt.flatMap(
            a ->
                a.getAnnotationMirrors().stream()
                    .filter(
                        m ->
                            ((QualifiedNameable) m.getAnnotationType().asElement())
                                .getQualifiedName()
                                .contentEquals(ModelRoot.class.getCanonicalName()))
                    .findAny());

    ClassName immutableModelRootType =
        parentModelRootOpt
            .flatMap(
                parentModelRoot ->
                    parentModelRootAnno.map(
                        annotationMirror ->
                            ClassName.get(
                                util.processingEnv()
                                    .getElementUtils()
                                    .getPackageOf(parentModelRoot)
                                    .getQualifiedName()
                                    .toString(),
                                parentModelRoot.getSimpleName()
                                    + util.getAnnotationElement(
                                        annotationMirror, "suffixSeparator", String.class)
                                    + IMMUT_SUFFIX)))
            .or(
                () ->
                    modelClusterRootAnno
                        .flatMap(anno -> util.getTypeFromAnnotationMember(anno::immutableRoot))
                        .map(tm -> (TypeElement) util.processingEnv().getTypeUtils().asElement(tm))
                        .map(ClassName::get))
            .orElse(ClassName.get(ImmutableModel.class));

    ClassName modelBuilderRootType =
        parentModelRootOpt
            .flatMap(
                parentModelRoot ->
                    parentModelRootAnno.map(
                        annotationMirror ->
                            ClassName.get(
                                util.processingEnv()
                                    .getElementUtils()
                                    .getPackageOf(parentModelRoot)
                                    .getQualifiedName()
                                    .toString(),
                                parentModelRoot.getSimpleName()
                                    + util.getAnnotationElement(
                                        annotationMirror, "suffixSeparator", String.class)
                                    + IMMUT_SUFFIX,
                                "Builder")))
            .or(
                () ->
                    modelClusterRootAnno
                        .flatMap(anno -> util.getTypeFromAnnotationMember(anno::builderRoot))
                        .map(tm -> (TypeElement) util.processingEnv().getTypeUtils().asElement(tm))
                        .map(ClassName::get))
            .orElse(ClassName.get(ImmutableModel.Builder.class));

    // Create the builder interface
    TypeSpec.Builder builderInterface =
        util.interfaceBuilder("Builder", modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(
                typeParamTypes.isEmpty()
                    ? modelBuilderRootType
                    : ParameterizedTypeName.get(
                        modelBuilderRootType,
                        typeParamTypes.stream().map(TypeName::get).toArray(TypeName[]::new)))
            .addMethods(
                generateBuilderInterfaceMethods(
                    modelMethods, immutableModelName, hasParentModelRoot));

    if (modelRoot.builderExtendsModelRoot()) {
      builderInterface.addSuperinterface(modelRootType.asType());
    }

    // Create the immutable interface
    return util.interfaceBuilder(
            immutableModelName.simpleName(), modelRootType.getQualifiedName().toString())
        .addModifiers(PUBLIC)
        .addSuperinterface(ClassName.get(modelRootType))
        .addSuperinterface(
            typeParamTypes.isEmpty()
                ? immutableModelRootType
                : ParameterizedTypeName.get(
                    immutableModelRootType,
                    typeParamTypes.stream().map(TypeName::get).toArray(TypeName[]::new)))
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_build", 0))
                .addModifiers(PUBLIC, DEFAULT)
                .returns(immutableModelName)
                .addStatement("return this")
                .build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(immutableModelName)
                .build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(immutableModelName.nestedClass("Builder"))
                .build())
        .addType(builderInterface.build())
        .build();
  }

  private Optional<TypeElement> getParentInterfaceWithAnnotation(
      TypeElement typeElement, Class<?> annotationClass) {
    return getInterfaceWithAnnotation(typeElement, annotationClass, true);
  }

  private Optional<TypeElement> getInterfaceWithAnnotation(
      TypeElement typeElement, Class<?> annotationClass) {
    return getInterfaceWithAnnotation(typeElement, annotationClass, false);
  }

  private Optional<TypeElement> getInterfaceWithAnnotation(
      TypeElement typeElement, Class<?> annotationClass, boolean skipFirst) {
    Optional<? extends AnnotationMirror> annotation =
        skipFirst
            ? Optional.empty()
            : typeElement.getAnnotationMirrors().stream()
                .filter(
                    m ->
                        ((QualifiedNameable) m.getAnnotationType().asElement())
                            .getQualifiedName()
                            .contentEquals(
                                requireNonNullElse(annotationClass.getCanonicalName(), "")))
                .findAny();
    if (annotation.isEmpty()) {
      @SuppressWarnings("UnnecessaryTypeArgument")
      List<TypeElement> list =
          typeElement.getInterfaces().stream()
              .map(t -> util.processingEnv().getTypeUtils().asElement(t))
              .filter(e -> e instanceof TypeElement)
              .map(e -> (TypeElement) e)
              .map(te -> getInterfaceWithAnnotation(requireNonNull(te), annotationClass, false))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .toList();
      if (list.isEmpty()) {
        return Optional.empty();
      } else if (list.size() > 1) {
        util.error(
            "More than one super interface has @%s annotation. Expected zero or one"
                .formatted(annotationClass.getSimpleName()),
            typeElement);
        return Optional.empty();
      } else {
        return Optional.ofNullable(list.get(0));
      }
    } else {
      return Optional.of(typeElement);
    }
  }

  /**
   * Generates methods for the builder interface.
   *
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param hasParentModelRoot Whether the modelRoot extends another model root (for example in case
   *     of a {@link Trait vajram trait} implementation)
   * @return List of method specs for the builder interface
   */
  private List<MethodSpec> generateBuilderInterfaceMethods(
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName,
      boolean hasParentModelRoot) {
    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      // Validate optional fields
      validateOptionalField(method);
      String methodName = method.getSimpleName().toString();

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC, ABSTRACT)
              .addParameter(util.getParameterType(method, true), methodName)
              .returns(ClassName.get("", "Builder"));
      if (hasParentModelRoot) {
        methodBuilder.addAnnotation(Override.class);
      }
      methods.add(methodBuilder.build());
    }

    methods.addAll(
        List.of(
            MethodSpec.overriding(util.getMethod(Model.class, "_build", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(immutableModelName)
                .build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(ClassName.get("", "Builder"))
                .build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC, DEFAULT)
                .returns(immutableModelName.nestedClass("Builder"))
                .addStatement("return this")
                .build()));

    return methods;
  }

  /**
   * Generates the immutable POJO class that implements the immutable interface.
   *
   * @param modelRootType The model root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name for the immutable POJO
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateImmutablePojo(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName,
      String immutablePojoName) {

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
        // Check if the method has @IfAbsent and is primitive
        IfAbsent ifAbsent = util.getIfAbsent(method);
        if (method.getReturnType().getKind().isPrimitive()
            && !ifAbsent.value().isMandatoryOnServer()) {
          fieldType = TypeName.get(method.getReturnType()).box();
        } else {
          fieldType = TypeName.get(method.getReturnType());
        }
      }

      // Add @Nullable annotation for Optional types or methods with @Nullable annotation
      if (util.isOptional(method.getReturnType())
          || util.isAnyNullable(method.getReturnType(), method)) {
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
          ParameterSpec.builder(util.getParameterType(method, false), fieldName).build());

      // For all field types, just assign the parameter directly
      constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
    }

    // Create getter methods for the POJO class
    List<MethodSpec> methods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      methods.add(getterMethod(method).build());
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
            .collect(Collectors.joining(", "));

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
        generateBuilderClass(modelRootType, modelMethods, immutableModelName, immutablePojoName);

    TypeSpec.Builder classBuilder =
        util.classBuilder(immutablePojoName, modelRootType.getQualifiedName().toString());
    util.addImmutableModelObjectMethods(
        immutableModelName,
        modelMethods.stream().map(ExecutableElement::getSimpleName).collect(Collectors.toSet()),
        classBuilder);
    // Create the POJO class
    return classBuilder
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(immutableModelName)
        .addFields(fields)
        .addMethod(constructorBuilder.build())
        .addMethods(methods)
        .addMethod(builderMethod)
        .addType(builderClass)
        .build();
  }

  private MethodSpec.Builder getterMethod(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    String methodName = method.getSimpleName().toString();
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(methodName)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(
                TypeName.get(returnType)
                    .annotated(
                        returnType.getAnnotationMirrors().stream()
                            .map(AnnotationSpec::get)
                            .toList()));
    // If the return type is Optional<T>, wrap the field in Optional.ofNullable()
    if (util.isOptional(returnType)) {
      methodBuilder.addStatement("return $T.ofNullable($N)", Optional.class, methodName);
    } else {
      if (util.getIfAbsent(method).value().usePlatformDefault()
          && !returnType.getKind().isPrimitive()) {
        try {
          methodBuilder.addCode(
              """
              if($N == null){
                return $L;
              }
              """,
              methodName,
              new DeclaredTypeVisitor(util, method)
                  .visit(returnType)
                  .defaultValueExpr(util.processingEnv()));
        } catch (CodeGenerationException e) {
          throw util.errorAndThrow(
              """
                  Could not find default value expression for specified type %s. \
                  Either the relevant type was not configured properly in a DataTypeFactory \
                  or the @IfAbsent() annotation is incorrectly specified.""",
              method);
        }
      }
      methodBuilder.addStatement("return $N", methodName);
    }
    return methodBuilder;
  }

  /**
   * Generates the builder class for the immutable POJO.
   *
   * @param modelRootType The model Root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name of the immutable POJO
   * @return TypeSpec for the builder class
   */
  private TypeSpec generateBuilderClass(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelName,
      String immutablePojoName) {
    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
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
        // Box primitive types for methods with @IfAbsent(FAIL) or with platform defaults
        if (method.getReturnType().getKind().isPrimitive()) {
          // Box primitive types for methods with @IfAbsent(FAIL) or platform defaults
          fieldType = TypeName.get(method.getReturnType()).box();
        } else {
          fieldType = TypeName.get(method.getReturnType());
        }
      }

      // Add @Nullable annotation for Optional types or methods with @Nullable annotation
      if (util.isOptional(method.getReturnType())
          || util.isAnyNullable(method.getReturnType(), method)) {
        // Add @Nullable as a type annotation
        TypeName annotatedType =
            fieldType.annotated(AnnotationSpec.builder(ClassName.get(Nullable.class)).build());
        fields.add(FieldSpec.builder(annotatedType, fieldName, PRIVATE).build());
      } else {
        fields.add(FieldSpec.builder(fieldType, fieldName, PRIVATE).build());
      }
    }

    // Create no-arg constructor
    MethodSpec noArgConstructor = MethodSpec.constructorBuilder().addModifiers(PRIVATE).build();

    // Create setter methods
    List<MethodSpec> dataAccessMethods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      dataAccessMethods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addParameter(util.getParameterType(method, true), methodName)
              .returns(ClassName.get("", "Builder"))
              .addStatement("this.$L = $L", methodName, methodName)
              .addStatement("return this")
              .addAnnotation(Override.class)
              .build());
      if (modelRoot.builderExtendsModelRoot()) {
        dataAccessMethods.add(getterMethod(method).build());
      }
    }

    // Create _build method
    MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("_build")
            .addModifiers(PUBLIC)
            .returns(ClassName.get("", immutablePojoName))
            .addAnnotation(Override.class);

    // Validate fields based on IfAbsent annotation strategies
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();
      TypeMirror actualType = util.getOptionalInnerType(returnType);
      CodeGenType dataType = new DeclaredTypeVisitor(util, method).visit(actualType, null);

      // If IfAbsent annotation was found, handle according to strategy
      // Only generate null check if validation is needed or error needs to be thrown
      if (!typeSupportsAbsentValues(method)) {
        buildMethodBuilder.beginControlFlow("if (this.$N == null)", fieldName);

        IfAbsentThen ifAbsentThen = util.getIfAbsent(method).value();
        switch (ifAbsentThen) {
          case FAIL ->
              // FAIL strategy - throw exception when value is null
              buildMethodBuilder.addStatement(
                  "throw new $T($S, $S)",
                  MandatoryFieldMissingException.class,
                  immutableModelName.simpleName(),
                  fieldName);
          case ASSUME_DEFAULT_VALUE -> {
            try {
              buildMethodBuilder.addStatement(
                  "this.$N = $L", fieldName, dataType.defaultValueExpr(util.processingEnv()));
            } catch (CodeGenerationException e) {
              throw util.errorAndThrow(
                  "Could not find default value expression for type '%s'".formatted(dataType),
                  method);
            }
          }
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
    return util.classBuilder("Builder", modelRootType.getQualifiedName().toString())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(immutableModelName.nestedClass("Builder"))
        .addFields(fields)
        .addMethod(noArgConstructor)
        .addMethods(dataAccessMethods)
        .addMethod(buildMethodBuilder.build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC)
                .returns(ClassName.get("", immutablePojoName + ".Builder"))
                .addStatement("return this")
                .build())
        .addMethod(builderCopyMethodBuilder.build())
        .build();
  }

  /**
   * Validates that fields not mandatory on server are not primitive types and are of Optional type
   * or are properly annotated as Nullable.
   */
  private void validateOptionalField(ExecutableElement method) {
    IfAbsentThen ifAbsentThen = util.getIfAbsent(method).value();
    ModelRoot modelRoot =
        requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
    if (modelRoot.type() == ModelType.REQUEST && !ifAbsentThen.isMandatoryOnServer()) {
      if (!typeSupportsAbsentValues(method)) {
        util.error(
            "Field '%s' with @IfAbsent(%s) must be an Optional or annotated with %s: "
                .formatted(method.getSimpleName(), ifAbsentThen, Nullable.class.getCanonicalName()),
            method);
      }
    }
  }

  private boolean typeSupportsAbsentValues(ExecutableElement method) {
    TypeMirror returnType = method.getReturnType();
    return !returnType.getKind().isPrimitive()
        && (util.isOptional(returnType) || util.isAnyNullable(returnType, method));
  }
}
