package com.flipkart.krystal.vajram.codegen.common.generators;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType.LIST;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType.NO_CONTAINER;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.asClassName;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.asTypeNameWithTypes;
import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.withTypeParams;
import static com.flipkart.krystal.codegen.common.models.CodegenPhase.MODELS;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.ASSUME_DEFAULT_VALUE;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.MAY_FAIL_CONDITIONALLY;
import static com.flipkart.krystal.model.PlainJavaObject.POJO;
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
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ContainerType;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelFieldTypeInfo;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodeGenerationException;
import com.flipkart.krystal.codegen.common.models.DeclaredTypeVisitor;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.MandatoryFieldMissingException;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelClusterRoot;
import com.flipkart.krystal.model.ModelProtocol;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.ModelUtils;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.model.list.ModelsListBuilder;
import com.flipkart.krystal.model.list.ModelsListView;
import com.flipkart.krystal.model.list.UnmodifiableModelsList;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.model.map.ModelsMapView;
import com.flipkart.krystal.model.map.UnmodifiableModelsMap;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerialId;
import com.flipkart.krystal.vajram.Trait;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
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
 * In addition, if the Model Root has the {@link SupportedModelProtocols} annotation and {@link
 * SupportedModelProtocols#value()} contains {@link PlainJavaObject}, then this generator also
 * generates the following classes in the same package as the model root:
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
 *       calls the all arg constructor of the Builder mentioned below. {@link Model#_newCopy()}
 *       returns {@code this} (inherited from {@link ImmutableModel}) since immutable objects need
 *       no copying.
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
public final class JavaModelsGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;
  private final ModelRoot modelRoot;

  public JavaModelsGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
    this.modelRoot = requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
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

    TypeElement modelRootType = codeGenContext.modelRootType();

    // For enum models: validate only, no code generation needed
    if (util.isEnumModel(modelRootType)) {
      validateEnumModel(modelRootType);
      return;
    }

    validate();

    CodeGenUtility util = codeGenContext.util();

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    // Validate @SerialId all-or-none consistency
    validateSerialIdConsistency(modelMethods);

    // Validate pure model constraints (nested Models must also be pure)
    if (modelRoot.pure()) {
      validatePureModel(modelMethods);
    }

    // Validate enum map keys not used with serde protocols
    validateNoEnumMapKeysForSerdeProtocols(modelRootType, modelMethods);

    // Validate nested model type consistency (REQUEST/RESPONSE)
    validateNestedModelTypeConsistency(modelRootType, modelMethods);

    // Get package and class names
    ClassName immutModelNameRaw = util.getImmutInterfaceName(modelRootType);
    String packageName = immutModelNameRaw.packageName();

    // Generate the immutable interface and its builder interface
    TypeSpec immutableInterface =
        generateImmutableInterface(modelRootType, modelMethods, immutModelNameRaw);
    // Write the immutable interface to a file
    util.writeJavaFile(packageName, immutableInterface, modelRootType);

    if (util.typeExplicitlySupportsProtocol(modelRootType, PlainJavaObject.class)) {
      // Generate the POJO class only if PlainJavaObject is explicitly supported
      util.writeJavaFile(
          packageName, generateImmutablePojo(modelRootType, modelMethods), modelRootType);
    }
  }

  private boolean isApplicable() {
    return MODELS.equals(codeGenContext.codegenPhase());
  }

  private void validate() {
    validateModelRoot(codeGenContext.modelRootType(), codeGenContext.util());
  }

  /**
   * Validates that all fields in a pure model are of allowed types. This validation is
   * protocol-independent and applies to any model with {@code @ModelRoot(pure = true)}.
   *
   * <p>Allowed types: primitives, boxed primitives, String, PrimitiveArray subtypes, pure Models,
   * Lists of these, or Maps with primitive/String keys and values of these types.
   */
  private void validatePureModel(List<ExecutableElement> modelMethods) {
    for (ExecutableElement method : modelMethods) {
      TypeMirror returnType = method.getReturnType();
      if (util.isOptional(returnType)) {
        returnType = util.getOptionalInnerType(returnType);
      }
      validatePureFieldType(method, returnType);
    }
  }

  private void validatePureFieldType(ExecutableElement method, TypeMirror type) {
    String fieldName = method.getSimpleName().toString();

    if (util.isPrimitiveOrBoxed(type)) {
      return;
    }
    if (util.isString(type)) {
      return;
    }
    if (util.isPrimitiveArray(type)) {
      return;
    }
    // Only enums with @ModelRoot annotation are allowed in pure models
    Element typeElement = util.processingEnv().getTypeUtils().asElement(type);
    if (typeElement != null
        && typeElement.getKind() == ElementKind.ENUM
        && typeElement.getAnnotation(ModelRoot.class) != null) {
      return;
    }
    if (util.isRawAssignable(type, Model.class)) {
      checkModelIsPure(method, type, fieldName);
      return;
    }
    if (util.isListType(type)) {
      TypeMirror elementType = util.getContentType(type);
      validatePureListElementType(method, elementType, fieldName);
      return;
    }
    if (util.isMapType(type)) {
      TypeMirror keyType = util.getMapKeyType(type);
      TypeMirror valueType = util.getMapValueType(type);
      validatePureMapTypes(method, keyType, valueType, fieldName);
      return;
    }

    util.error(
        "Field '%s' in pure model '%s' has disallowed type '%s'. "
                .formatted(fieldName, codeGenContext.modelRootType().getQualifiedName(), type)
            + "Pure models only allow primitives, boxed primitives, String, enums, PrimitiveArray subtypes, pure Models, "
            + "Lists of these, or Maps with primitive/String/enum keys and primitive/String/enum/pure Model values.",
        method);
  }

  private void checkModelIsPure(ExecutableElement method, TypeMirror type, String fieldName) {
    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (element instanceof TypeElement typeElement) {
      ModelRoot fieldModelRoot = typeElement.getAnnotation(ModelRoot.class);
      if (fieldModelRoot == null) {
        util.error(
            "Field '%s' in pure model '%s' references type '%s' which extends Model but does not have @ModelRoot annotation."
                .formatted(
                    fieldName,
                    codeGenContext.modelRootType().getQualifiedName(),
                    typeElement.getQualifiedName()),
            method);
      } else if (!fieldModelRoot.pure()) {
        util.error(
            "Field '%s' in pure model '%s' references model '%s' which is not pure (pure = false). All Model fields in a pure model must themselves be pure."
                .formatted(
                    fieldName,
                    codeGenContext.modelRootType().getQualifiedName(),
                    typeElement.getQualifiedName()),
            method);
      }
    }
  }

  private void validatePureListElementType(
      ExecutableElement method, TypeMirror elementType, String fieldName) {
    if (util.isPrimitiveOrBoxed(elementType) || util.isString(elementType)) {
      return;
    }
    Element elemElement = util.processingEnv().getTypeUtils().asElement(elementType);
    if (elemElement != null
        && elemElement.getKind() == ElementKind.ENUM
        && elemElement.getAnnotation(ModelRoot.class) != null) {
      return;
    }
    if (util.isRawAssignable(elementType, Model.class)) {
      checkModelIsPure(method, elementType, fieldName);
      return;
    }
    util.error(
        "Field '%s' in pure model '%s' has List with disallowed element type '%s'. "
                .formatted(
                    fieldName, codeGenContext.modelRootType().getQualifiedName(), elementType)
            + "List elements in pure models must be primitives, boxed primitives, String, enums, or pure Models.",
        method);
  }

  private void validatePureMapTypes(
      ExecutableElement method, TypeMirror keyType, TypeMirror valueType, String fieldName) {
    if (util.isPrimitiveOrBoxed(valueType) || util.isString(valueType)) {
      return;
    }
    Element valElement = util.processingEnv().getTypeUtils().asElement(valueType);
    if (valElement != null
        && valElement.getKind() == ElementKind.ENUM
        && valElement.getAnnotation(ModelRoot.class) != null) {
      return;
    }
    if (util.isRawAssignable(valueType, Model.class)) {
      checkModelIsPure(method, valueType, fieldName);
      return;
    }
    util.error(
        "Field '%s' in pure model '%s' has Map with disallowed value type '%s'. "
                .formatted(fieldName, codeGenContext.modelRootType().getQualifiedName(), valueType)
            + "Map values in pure models must be primitives, boxed primitives, String, enums, or pure Models.",
        method);
  }

  /**
   * Validates that enum models are not used as map keys in models that support serde protocols.
   * Enum map keys have problematic unknown-key semantics during deserialization. For non-serde
   * (model-only) protocols like {@link PlainJavaObject}, enum map keys are allowed.
   */
  private void validateNoEnumMapKeysForSerdeProtocols(
      TypeElement modelRootType, List<ExecutableElement> modelMethods) {
    SupportedModelProtocols supportedModelProtocols =
        modelRootType.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols == null) {
      return;
    }
    boolean hasSerdeProtocol =
        util.getTypeElemsFromAnnotationMember(supportedModelProtocols::value).stream()
            .anyMatch(tm -> util.isRawAssignable(tm.asType(), SerdeProtocol.class));
    if (!hasSerdeProtocol) {
      return;
    }
    for (ExecutableElement method : modelMethods) {
      TypeMirror returnType = method.getReturnType();
      if (util.isOptional(returnType)) {
        returnType = util.getOptionalInnerType(returnType);
      }
      if (util.isMapType(returnType)) {
        TypeMirror keyType = util.getMapKeyType(returnType);
        if (util.isEnumModelType(keyType)) {
          util.error(
              "Field '%s' in model '%s' uses an EnumModel ('%s') as a map key. "
                      .formatted(method.getSimpleName(), modelRootType.getQualifiedName(), keyType)
                  + "EnumModel map keys are not allowed in models that support serde protocols "
                  + "because unknown enum keys cannot be deserialized reliably.",
              method);
        }
      }
    }
  }

  /**
   * Validates that REQUEST models only contain nested models that include REQUEST in their type,
   * and RESPONSE models only contain nested models that include RESPONSE in their type. Models with
   * empty type have no restriction.
   */
  private void validateNestedModelTypeConsistency(
      TypeElement modelRootType, List<ExecutableElement> modelMethods) {
    Set<ModelType> parentTypes = Set.of(modelRoot.type());
    if (parentTypes.isEmpty()) {
      return;
    }
    for (ExecutableElement method : modelMethods) {
      TypeMirror returnType = method.getReturnType();
      if (util.isOptional(returnType)) {
        returnType = util.getOptionalInnerType(returnType);
      }
      if (util.isListType(returnType)) {
        returnType = util.getContentType(returnType);
      } else if (util.isMapType(returnType)) {
        returnType = util.getMapValueType(returnType);
      }
      checkNestedModelType(method, returnType, modelRootType, parentTypes);
    }
  }

  private void checkNestedModelType(
      ExecutableElement method,
      TypeMirror fieldType,
      TypeElement modelRootType,
      Set<ModelType> parentTypes) {
    if (!util.isRawAssignable(fieldType, Model.class)) {
      return;
    }
    Element element = util.processingEnv().getTypeUtils().asElement(fieldType);
    if (!(element instanceof TypeElement fieldTypeElement)) {
      return;
    }
    ModelRoot fieldModelRoot = fieldTypeElement.getAnnotation(ModelRoot.class);
    if (fieldModelRoot == null) {
      return;
    }
    Set<ModelType> fieldTypes = Set.of(fieldModelRoot.type());
    // A nested model with empty type can be nested anywhere
    if (fieldTypes.isEmpty()) {
      return;
    }
    // For each parent type, the nested model must also include that type
    for (ModelType parentType : parentTypes) {
      if (!fieldTypes.contains(parentType)) {
        util.error(
            "Field '%s' in %s model '%s' references model '%s' which does not include %s in its type. "
                    .formatted(
                        method.getSimpleName(),
                        parentType,
                        modelRootType.getQualifiedName(),
                        fieldTypeElement.getQualifiedName(),
                        parentType)
                + "%s models can only contain nested models that also include %s in their type."
                    .formatted(parentType, parentType),
            method);
      }
    }
  }

  /**
   * Validates that @SerialId is either present on all model methods or none. Partial usage is not
   * allowed.
   */
  private void validateSerialIdConsistency(List<ExecutableElement> modelMethods) {
    boolean anyHasSerialId =
        modelMethods.stream().anyMatch(m -> m.getAnnotation(SerialId.class) != null);
    if (anyHasSerialId) {
      for (ExecutableElement method : modelMethods) {
        if (method.getAnnotation(SerialId.class) == null) {
          util.error(
              "Model '%s': @SerialId must be present on all fields or none. Field '%s' is missing @SerialId"
                  .formatted(
                      codeGenContext.modelRootType().getQualifiedName(), method.getSimpleName()),
              method);
        }
      }
    }
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

    if (!extendsModel(modelRootType, util)) {
      util.error(
          "Interface with @ModelRoot annotation must extend " + Model.class.getCanonicalName(),
          modelRootType);
    }
  }

  /**
   * Validates enum model constraints:
   *
   * <ul>
   *   <li>Must implement {@link EnumModel}
   *   <li>First enum constant must be UNKNOWN
   *   <li>If any constant has @SerialId, UNKNOWN must have @SerialId(0)
   * </ul>
   */
  private void validateEnumModel(TypeElement enumType) {
    // Validate that the enum implements EnumModel
    if (!util.isRawAssignable(enumType.asType(), EnumModel.class)) {
      util.error(
          "Enum '%s' with @ModelRoot annotation must implement %s"
              .formatted(enumType.getQualifiedName(), EnumModel.class.getCanonicalName()),
          enumType);
    }

    // Validate that builderExtendsModelRoot is not used on enum models
    ModelRoot modelRoot = enumType.getAnnotation(ModelRoot.class);
    if (modelRoot != null && modelRoot.builderExtendsModelRoot()) {
      util.error(
          "Enum '%s' with @ModelRoot annotation must not have builderExtendsModelRoot = true"
              .formatted(enumType.getQualifiedName()),
          enumType);
    }

    // Get enum constants in declaration order
    List<VariableElement> enumConstants =
        ElementFilter.fieldsIn(enumType.getEnclosedElements()).stream()
            .filter(field -> field.getKind() == ElementKind.ENUM_CONSTANT)
            .toList();

    if (enumConstants.isEmpty()) {
      util.error(
          "Enum '%s' with @ModelRoot annotation must have at least one constant (UNKNOWN)"
              .formatted(enumType.getQualifiedName()),
          enumType);
      return;
    }

    // Validate UNKNOWN is the first constant
    VariableElement firstConstant = enumConstants.get(0);
    if (!firstConstant.getSimpleName().contentEquals("UNKNOWN")) {
      util.error(
          "Enum '%s' with @ModelRoot annotation must have 'UNKNOWN' as the first constant, but found '%s'"
              .formatted(enumType.getQualifiedName(), firstConstant.getSimpleName()),
          firstConstant);
    }

    // Check @SerialId usage
    boolean anyHasSerialId =
        enumConstants.stream().anyMatch(c -> c.getAnnotation(SerialId.class) != null);

    if (anyHasSerialId) {
      // All-or-none: if any constant has @SerialId, all must have it
      for (VariableElement constant : enumConstants) {
        if (constant.getAnnotation(SerialId.class) == null) {
          util.error(
              "Enum '%s': @SerialId must be present on all constants or none. Constant '%s' is missing @SerialId"
                  .formatted(enumType.getQualifiedName(), constant.getSimpleName()),
              constant);
        }
      }

      // If any constant has @SerialId, UNKNOWN must have @SerialId(0)
      SerialId unknownSerialId = firstConstant.getAnnotation(SerialId.class);
      if (unknownSerialId == null) {
        util.error(
            "Enum '%s': When @SerialId is used on any constant, UNKNOWN must have @SerialId(0)"
                .formatted(enumType.getQualifiedName()),
            firstConstant);
      } else if (unknownSerialId.value() != 0) {
        util.error(
            "Enum '%s': UNKNOWN must have @SerialId(0), but found @SerialId(%d)"
                .formatted(enumType.getQualifiedName(), unknownSerialId.value()),
            firstConstant);
      }

      // Validate no duplicate @SerialId values
      Set<Integer> usedIds = new HashSet<>();
      for (VariableElement constant : enumConstants) {
        SerialId serialId = constant.getAnnotation(SerialId.class);
        if (serialId != null) {
          if (!usedIds.add(serialId.value())) {
            util.error(
                "Enum '%s': Duplicate @SerialId(%d) on constant '%s'"
                    .formatted(
                        enumType.getQualifiedName(), serialId.value(), constant.getSimpleName()),
                constant);
          }
          if (serialId.value() < 0) {
            util.error(
                "Enum '%s': @SerialId must be non-negative, but found @SerialId(%d) on constant '%s'"
                    .formatted(
                        enumType.getQualifiedName(), serialId.value(), constant.getSimpleName()),
                constant);
          }
        }
      }
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
   * @param immutableModelNameRaw The name for the immutable interface without type params
   * @return TypeSpec for the immutable interface
   */
  private TypeSpec generateImmutableInterface(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      ClassName immutableModelNameRaw) {

    TypeName immutableModelName =
        withTypeParams(immutableModelNameRaw, modelRootType.getTypeParameters());

    TypeName builderType =
        withTypeParams(
            immutableModelNameRaw.nestedClass("Builder"), modelRootType.getTypeParameters());

    List<TypeVariableName> typeVariableNames =
        modelRootType.getTypeParameters().stream().map(TypeVariableName::get).toList();

    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    Optional<TypeMirror> parentModelRootTypeOpt =
        getParentInterfaceWithAnnotation(modelRootType, ModelRoot.class);
    boolean hasParentModelRoot = parentModelRootTypeOpt.isPresent();
    Optional<TypeElement> modelClusterRoot =
        parentModelRootTypeOpt.isPresent()
            ? Optional.empty()
            : getInterfaceWithAnnotation(modelRootType, ModelClusterRoot.class);
    ImmutableList<TypeMirror> typeParamTypes =
        modelClusterRoot
            .map(mcr -> util.getTypeParamTypes(modelRootType, mcr))
            .orElse(ImmutableList.of());

    Optional<ModelClusterRoot> modelClusterRootAnno =
        modelClusterRoot.map(typeElement -> typeElement.getAnnotation(ModelClusterRoot.class));

    Optional<? extends AnnotationMirror> parentModelRootAnno =
        parentModelRootTypeOpt
            .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
            .flatMap(
                parentModelRootElem ->
                    parentModelRootElem.getAnnotationMirrors().stream()
                        .filter(
                            m ->
                                ((QualifiedNameable) m.getAnnotationType().asElement())
                                    .getQualifiedName()
                                    .contentEquals(ModelRoot.class.getCanonicalName()))
                        .findAny());
    TypeName immutableModelRootType =
        parentModelRootTypeOpt
            .flatMap(
                parentModelRootType -> {
                  List<? extends TypeMirror> typeArguments;
                  if (parentModelRootType instanceof DeclaredType declaredType) {
                    typeArguments = declaredType.getTypeArguments();
                  } else {
                    typeArguments = List.of();
                  }
                  return parentModelRootAnno.map(
                      annotationMirror -> {
                        Element modelRootElement =
                            util.processingEnv().getTypeUtils().asElement(parentModelRootType);
                        return asTypeNameWithTypes(
                            util.getImmutInterfaceName(modelRootElement), typeArguments);
                      });
                })
            .or(
                () ->
                    modelClusterRootAnno
                        .map(anno -> util.getTypeElemFromAnnotationMember(anno::immutableRoot))
                        .map(ClassName::get))
            .orElse(ClassName.get(ImmutableModel.class));

    TypeName modelBuilderRootType =
        parentModelRootTypeOpt
            .flatMap(
                parentModelRootType -> {
                  List<? extends TypeMirror> typeArguments;
                  if (parentModelRootType instanceof DeclaredType declaredType) {
                    typeArguments = declaredType.getTypeArguments();
                  } else {
                    typeArguments = List.of();
                  }
                  return parentModelRootAnno.map(
                      annotationMirror -> {
                        Element parentModelRoot =
                            util.processingEnv().getTypeUtils().asElement(parentModelRootType);
                        return asTypeNameWithTypes(
                            util.getImmutInterfaceName(parentModelRoot).nestedClass("Builder"),
                            typeArguments);
                      });
                })
            .or(
                () ->
                    modelClusterRootAnno
                        .map(anno -> util.getTypeElemFromAnnotationMember(anno::builderRoot))
                        .map(ClassName::get))
            .orElse(ClassName.get(Builder.class));

    // Create the builder interface
    TypeSpec.Builder builderInterface =
        util.interfaceBuilder(
                "Builder", typeVariableNames, modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC, STATIC)
            .addSuperinterface(asTypeNameWithTypes(modelBuilderRootType, typeParamTypes))
            .addMethods(
                generateBuilderInterfaceMethods(
                    modelMethods, immutableModelName, hasParentModelRoot, builderType));

    if (modelRoot.builderExtendsModelRoot()) {
      builderInterface.addSuperinterface(modelRootType.asType());
    }

    // Generate getter overrides for @IfAbsent(FAIL) fields to strip @Nullable in _Immut interface
    List<MethodSpec> immutGetterOverrides = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      if (isIfAbsentFail(method, util, modelRoot)
          && util.isAnyNullable(method.getReturnType(), method)
          && !util.isOptional(method.getReturnType())) {
        TypeName returnType =
            TypeName.get(method.getReturnType())
                .annotated(
                    method.getReturnType().getAnnotationMirrors().stream()
                        .map(AnnotationSpec::get)
                        .filter(a -> !isNullableAnnotation(a))
                        .toList());
        immutGetterOverrides.add(
            MethodSpec.methodBuilder(method.getSimpleName().toString())
                .addModifiers(PUBLIC, ABSTRACT)
                .addAnnotation(Override.class)
                .returns(returnType)
                .build());
      }
    }

    // Create the immutable interface
    TypeName modelRootTypeName =
        withTypeParams(ClassName.get(modelRootType), modelRootType.getTypeParameters());

    return util.interfaceBuilder(
            asClassName(immutableModelName).simpleName(),
            typeVariableNames,
            modelRootType.getQualifiedName().toString())
        .addModifiers(PUBLIC)
        .addSuperinterface(modelRootTypeName)
        .addSuperinterface(asTypeNameWithTypes(immutableModelRootType, typeParamTypes))
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_build", 0))
                .addModifiers(PUBLIC, DEFAULT)
                .returns(immutableModelName)
                .addStatement("return this")
                .build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(PUBLIC, DEFAULT)
                .returns(immutableModelName)
                .addStatement("return this")
                .build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(builderType)
                .build())
        .addMethods(immutGetterOverrides)
        .addType(builderInterface.build())
        .build();
  }

  @SuppressWarnings("SameParameterValue")
  private Optional<TypeMirror> getParentInterfaceWithAnnotation(
      TypeElement typeElement, Class<?> annotationClass) {
    return getInterfaceWithAnnotation(typeElement.asType(), annotationClass, true);
  }

  @SuppressWarnings("SameParameterValue")
  private Optional<TypeElement> getInterfaceWithAnnotation(
      TypeElement typeElement, Class<?> annotationClass) {
    Optional<TypeMirror> interfaceWithAnnotation =
        getInterfaceWithAnnotation(typeElement.asType(), annotationClass, false);
    if (interfaceWithAnnotation.isPresent()) {
      if (util.processingEnv().getTypeUtils().asElement(interfaceWithAnnotation.get())
          instanceof TypeElement t) {
        return Optional.of(t);
      }
    }
    return Optional.empty();
  }

  private Optional<TypeMirror> getInterfaceWithAnnotation(
      TypeMirror typeMirror, Class<?> annotationClass, boolean skipFirst) {
    Element element = util.processingEnv().getTypeUtils().asElement(typeMirror);
    Optional<? extends AnnotationMirror> annotation =
        skipFirst
            ? Optional.empty()
            : element.getAnnotationMirrors().stream()
                .filter(
                    m ->
                        ((QualifiedNameable) m.getAnnotationType().asElement())
                            .getQualifiedName()
                            .contentEquals(
                                requireNonNullElse(annotationClass.getCanonicalName(), "")))
                .findAny();
    if (annotation.isEmpty()) {
      @SuppressWarnings("UnnecessaryTypeArgument")
      List<TypeMirror> list = new ArrayList<>();
      if (element instanceof TypeElement typeElement) {
        for (TypeMirror t : typeElement.getInterfaces()) {
          Element e = util.processingEnv().getTypeUtils().asElement(t);
          if (e instanceof TypeElement) {
            getInterfaceWithAnnotation(t, annotationClass, false).ifPresent(list::add);
          }
        }
      }
      if (list.isEmpty()) {
        return Optional.empty();
      } else if (list.size() > 1) {
        util.error(
            "More than one super interface has @%s annotation. Expected zero or one"
                .formatted(annotationClass.getSimpleName()),
            element);
        return Optional.empty();
      } else {
        return Optional.ofNullable(list.get(0));
      }
    } else {
      return Optional.of(typeMirror);
    }
  }

  /**
   * Generates methods for the builder interface.
   *
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param hasParentModelRoot Whether the modelRoot extends another model root (for example in case
   *     of a {@link Trait vajram trait} implementation)
   * @param builderType
   * @return List of method specs for the builder interface
   */
  private List<MethodSpec> generateBuilderInterfaceMethods(
      List<ExecutableElement> modelMethods,
      TypeName immutableModelName,
      boolean hasParentModelRoot,
      TypeName builderType) {
    List<MethodSpec> methods = new ArrayList<>();

    for (ExecutableElement method : modelMethods) {
      // Validate optional fields
      validateOptionalField(method);
      String methodName = method.getSimpleName().toString();

      TypeName variableType = util.getVariableType(method, true);
      MethodSpec.Builder setter =
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC, ABSTRACT)
              .addParameter(variableType, methodName)
              .returns(builderType);
      if (hasParentModelRoot) {
        setter.addAnnotation(Override.class);
      }
      methods.add(setter.build());

      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      if (fieldModelRootInfo.isPresent()
          && !fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
          && !util.isEnumModel(fieldModelRootInfo.get().element())
          && NO_CONTAINER.equals(fieldModelRootInfo.get().containerType())) {
        MethodSpec.Builder methodBuilder =
            MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC, ABSTRACT)
                .addParameter(
                    util.getImmutInterfaceName(fieldModelRootInfo.get().element())
                        .nestedClass("Builder"),
                    methodName)
                .returns(builderType);
        if (hasParentModelRoot) {
          methodBuilder.addAnnotation(Override.class);
        }
        methods.add(methodBuilder.build());
      }
      if (fieldModelRootInfo.isPresent()
          && !util.isEnumModel(fieldModelRootInfo.get().element())
          && fieldModelRootInfo.get().containerType().isContainer()) {
        methods.add(
            MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(TypeName.get(method.getReturnType()))
                .build());
      }
    }

    methods.addAll(
        List.of(
            MethodSpec.overriding(util.getMethod(Model.class, "_build", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(immutableModelName)
                .build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
                .addModifiers(PUBLIC, ABSTRACT)
                .returns(builderType)
                .build(),
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC, DEFAULT)
                .returns(builderType)
                .addStatement("return this")
                .build()));

    return methods;
  }

  /**
   * Generates the immutable POJO class that implements the immutable interface.
   *
   * @param modelRootType The model root type
   * @param modelMethods The methods from the model root
   * @return TypeSpec for the immutable POJO
   */
  private TypeSpec generateImmutablePojo(
      TypeElement modelRootType, List<ExecutableElement> modelMethods) {
    ModelRoot modelRoot = requireNonNull(modelRootType.getAnnotation(ModelRoot.class));

    ClassName immutInterfaceNameRaw = util.getImmutInterfaceName(modelRootType);
    ClassName immutablePojoNameRaw = util.getImmutClassName(modelRootType, POJO);
    TypeName immutIfaceType =
        withTypeParams(immutInterfaceNameRaw, modelRootType.getTypeParameters());
    TypeName immutPojoType =
        withTypeParams(immutablePojoNameRaw, modelRootType.getTypeParameters());
    TypeName builderType =
        withTypeParams(
            immutablePojoNameRaw.nestedClass("Builder"), modelRootType.getTypeParameters());
    List<TypeVariableName> typeVariableNames =
        modelRootType.getTypeParameters().stream().map(TypeVariableName::get).toList();
    // Create fields for the POJO class
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      fields.add(
          FieldSpec.builder(
                  util.getModelFieldType(method, false, null).fieldType(),
                  method.getSimpleName().toString(),
                  PRIVATE,
                  FINAL)
              .build());
    }

    // Create getter methods for the POJO class
    List<MethodSpec> methods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      methods.add(
          getterMethod(method, false, null, util, immutPojoType, modelRoot)
              .addAnnotation(Override.class)
              .build());
    }

    MethodSpec.Builder asBuilderMethodBuilder = asBuilder(modelMethods, builderType);

    // Add methods to the list
    methods.add(asBuilderMethodBuilder.build());

    // Add _newCopy with refined return type
    methods.add(
        MethodSpec.overriding(util.getMethod(Model.class, "_newCopy", 0))
            .addModifiers(PUBLIC)
            .returns(immutPojoType)
            .addStatement("return this")
            .build());

    // Create _builder static method
    MethodSpec builderMethod =
        MethodSpec.methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .addTypeVariables(typeVariableNames)
            .returns(builderType)
            .addStatement("return new $T()", builderType)
            .build();

    // Create builder class
    TypeSpec builderClass =
        generateBuilderClass(
            modelRootType, modelMethods, immutIfaceType, immutPojoType, builderType);

    TypeSpec.Builder classBuilder =
        util.classBuilder(
            asClassName(immutPojoType).simpleName(),
            typeVariableNames,
            modelRootType.getQualifiedName().toString());
    util.addImmutableModelObjectMethods(
        asClassName(immutIfaceType),
        modelMethods.stream().map(ExecutableElement::getSimpleName).collect(Collectors.toSet()),
        classBuilder);
    // Create the POJO class
    return classBuilder
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(immutIfaceType)
        .addFields(fields)
        .addMethod(allArgCtor(modelMethods, util).build())
        .addMethod(copyCtor(modelRootType, util))
        .addMethods(methods)
        .addMethod(builderMethod)
        .addType(builderClass)
        .build();
  }

  public static MethodSpec.Builder asBuilder(
      List<ExecutableElement> modelMethods, TypeName builderType) {
    // Create _asBuilder method to return a new Builder instance with all fields
    MethodSpec.Builder asBuilderMethodBuilder =
        MethodSpec.methodBuilder("_asBuilder")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(builderType);

    // Initialize code to create a new Builder and set all fields
    asBuilderMethodBuilder.addCode("return new $T()", builderType);
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      asBuilderMethodBuilder.addCode(".$L($L)", fieldName, fieldName);
    }
    asBuilderMethodBuilder.addCode(";");
    return asBuilderMethodBuilder;
  }

  private static MethodSpec.Builder allArgCtor(
      List<ExecutableElement> modelMethods, CodeGenUtility util) {
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      constructorBuilder.addParameter(
          ParameterSpec.builder(util.getVariableType(method, false), fieldName).build());
      ContainerType containerType = util.getContainerType(method.getReturnType());
      Optional<ModelRootInfo> modelRootInfo = util.asModelRoot(method.getReturnType(), method);
      switch (containerType) {
        case NO_CONTAINER -> {
          if (modelRootInfo.isPresent()) {
            constructorBuilder.addStatement(
                "this.$L = $L == null ? null : ($T)$L._build()",
                fieldName,
                fieldName,
                util.getModelFieldType(method, false, null).fieldType(),
                fieldName);
          } else {
            constructorBuilder.addStatement("this.$L = $L", fieldName, fieldName);
          }
        }
        case LIST -> {
          if (modelRootInfo.isPresent()) {
            constructorBuilder.addStatement(
"""
    this.$L = $L == null
        ? null
        : $T.copyOf(
            $T.transform($L, _e -> ($T) _e._build()))
""",
                fieldName,
                fieldName,
                ImmutableList.class,
                Lists.class,
                fieldName,
                util.getModelFieldType(method, false, null).elementType());
          } else {
            constructorBuilder.addStatement(
                "this.$L = $L == null ? null : $T.copyOf($L)",
                fieldName,
                fieldName,
                ImmutableList.class,
                fieldName);
          }
        }
        case MAP -> {
          if (modelRootInfo.isPresent()) {
            constructorBuilder.addStatement(
"""
this.$L = $L == null
    ? null
    : $T.copyOf(
        $T.transformValues($L, _e -> ($T) _e._build()))
""",
                fieldName,
                fieldName,
                ImmutableMap.class,
                Maps.class,
                fieldName,
                util.getModelFieldType(method, false, null).elementType());
          } else {
            constructorBuilder.addStatement(
                "this.$L = $L == null ? null :$T.copyOf($L)",
                fieldName,
                fieldName,
                ImmutableMap.class,
                fieldName);
          }
        }
      }
    }
    return constructorBuilder;
  }

  public static MethodSpec copyCtor(TypeElement modelRootType, CodeGenUtility util) {
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);
    MethodSpec.Builder ctorBuilder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    ctorBuilder.addParameter(
        ParameterSpec.builder(TypeName.get(modelRootType.asType()), "_from").build());

    ctorBuilder.addCode("this(");
    ctorBuilder.addCode(
        modelMethods.stream()
            .map(
                method -> {
                  boolean isOptional = util.isOptional(method.getReturnType());
                  return CodeBlock.of(
                      "_from.$L()" + (isOptional ? ".orElse(null)" : ""),
                      method.getSimpleName().toString());
                })
            .collect(CodeBlock.joining(",")));
    ctorBuilder.addCode(");");
    return ctorBuilder.build();
  }

  /**
   * @param method
   * @param isBuilder
   * @param modelProtocol
   * @param util
   * @param modelTypeName Must be non-null if and only if the field is mandatory
   * @param modelRoot
   * @return
   */
  public static MethodSpec.Builder getterMethod(
      ExecutableElement method,
      boolean isBuilder,
      @Nullable ModelProtocol modelProtocol,
      CodeGenUtility util,
      TypeName modelTypeName,
      ModelRoot modelRoot) {
    TypeMirror specifiedReturnType = method.getReturnType();
    ModelFieldTypeInfo modelFieldType = util.getModelFieldType(method, true, null);
    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(specifiedReturnType, method);
    String fieldName = method.getSimpleName().toString();
    TypeName actualReturnType =
        isBuilder
                && fieldModelRootInfo.isPresent()
                && !util.isEnumModel(fieldModelRootInfo.get().element())
            ? switch (modelFieldType.containerType()) {
              case NO_CONTAINER -> TypeName.get(specifiedReturnType);
              case LIST ->
                  ParameterizedTypeName.get(
                      ClassName.get(UnmodifiableModelsList.class),
                      TypeName.get(fieldModelRootInfo.get().type()),
                      util.getImmutTypeName(fieldModelRootInfo.get().type(), modelProtocol));
              case MAP -> {
                TypeMirror mapKeyType = util.getMapKeyType(specifiedReturnType);
                yield ParameterizedTypeName.get(
                    ClassName.get(UnmodifiableModelsMap.class),
                    TypeName.get(mapKeyType),
                    TypeName.get(fieldModelRootInfo.get().type()),
                    util.getImmutTypeName(fieldModelRootInfo.get().type(), modelProtocol));
              }
            }
            : TypeName.get(specifiedReturnType)
                .annotated(
                    specifiedReturnType.getAnnotationMirrors().stream()
                        .map(AnnotationSpec::get)
                        .filter(
                            a ->
                                isBuilder
                                    || !isIfAbsentFail(method, util, modelRoot)
                                    || !isNullableAnnotation(a))
                        .toList());
    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(fieldName).addModifiers(PUBLIC).returns(actualReturnType);
    // If the return type is Optional<T>, wrap the field in Optional.ofNullable()
    if (util.isOptional(specifiedReturnType)) {
      methodBuilder.addStatement(
          "return $T.ofNullable($L)",
          Optional.class,
          getFieldAccessorExpression(isBuilder, fieldName, specifiedReturnType, method, util));
    } else {
      if (util.getIfAbsent(method, modelRoot).value().usePlatformDefault()
          && !specifiedReturnType.getKind().isPrimitive()) {
        try {
          methodBuilder.addCode(
"""
    if($N == null){
      return $L;
    }
""",
              fieldName,
              fieldModelRootInfo.isPresent()
                      && !util.isEnumModel(fieldModelRootInfo.get().element())
                      && LIST.equals(fieldModelRootInfo.get().containerType())
                  ? CodeBlock.of(
                      "$T.<$T, $T>empty().asModelsView()",
                      ModelsListView.class,
                      TypeName.get(fieldModelRootInfo.get().type()),
                      util.getImmutInterfaceName(fieldModelRootInfo.get().element()))
                  : fieldModelRootInfo.isPresent()
                          && !util.isEnumModel(fieldModelRootInfo.get().element())
                          && ContainerType.MAP.equals(fieldModelRootInfo.get().containerType())
                      ? CodeBlock.of(
                          "$T.<$T, $T, $T>empty().asModelsView()",
                          ModelsMapView.class,
                          TypeName.get(util.getMapKeyType(specifiedReturnType)),
                          TypeName.get(fieldModelRootInfo.get().type()),
                          util.getImmutInterfaceName(fieldModelRootInfo.get().element()))
                      : new DeclaredTypeVisitor(util, method)
                          .visit(specifiedReturnType)
                          .defaultValueExpr(util.processingEnv()));
        } catch (CodeGenerationException e) {
          throw util.errorAndThrow(
              """
                Could not find default value expression for specified type %s. \
                Either the relevant type was not configured properly in a DataTypeFactory \
                or the @IfAbsent() annotation is incorrectly specified."""
                  .formatted(specifiedReturnType),
              method);
        }
      }
      // For builders with builderExtendsModelRoot=true, check if field is NonNull
      // If so, throw MandatoryFieldMissingException instead of returning null
      // Exclude Lists/Maps of Models (they can be empty collections)
      if (isBuilder
          && modelRoot.builderExtendsModelRoot()
          && !isMethodOptionalOrNullable(method, util)
          && !fieldModelRootInfo
              .map(ModelRootInfo::containerType)
              .map(ContainerType::isContainer)
              .orElse(false)) {
        methodBuilder.addCode(
            CodeBlock.builder()
                .add(
"""
    if ($N == null) {
      throw new $T($S, $S);
    }
""",
                    fieldName,
                    MandatoryFieldMissingException.class,
                    modelTypeName,
                    fieldName)
                .build());
      }
      methodBuilder.addStatement(
          "return $L",
          getFieldAccessorExpression(isBuilder, fieldName, specifiedReturnType, method, util));
    }
    return methodBuilder;
  }

  private static CodeBlock getFieldAccessorExpression(
      boolean isBuilder,
      String fieldName,
      TypeMirror returnType,
      ExecutableElement method,
      CodeGenUtility util) {
    CodeBlock fieldAccessorCode = CodeBlock.of("$L", fieldName);

    Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(returnType, method);
    if (isBuilder
        && fieldModelRootInfo.isPresent()
        && !util.isEnumModel(fieldModelRootInfo.get().element())) {
      fieldAccessorCode =
          switch (fieldModelRootInfo.get().containerType()) {
            case NO_CONTAINER ->
                // If field builder extends model root, we can return the builder as is.
                // If field builder does not extend model root, we have to convert it into the model
                // root by calling build.
                fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
                    ? CodeBlock.of("$L", fieldName)
                    : CodeBlock.of(
                        """
                        $L instanceof $T _builder
                          ? _builder._build()
                          : $L instanceof $T _immutModel ? _immutModel : null
                        """,
                        fieldName,
                        util.getImmutInterfaceName(fieldModelRootInfo.get().element())
                            .nestedClass("Builder"),
                        fieldName,
                        util.getImmutInterfaceName(fieldModelRootInfo.get().element()));
            case LIST, MAP -> CodeBlock.of("$L.unmodifiableModelsView()", fieldName);
          };
    }
    return fieldAccessorCode;
  }

  /**
   * Generates the builder class for the immutable POJO.
   *
   * @param modelRootType The model Root type
   * @param modelMethods The methods from the model root
   * @param immutableModelName The name of the immutable interface
   * @param immutablePojoName The name of the immutable POJO
   * @param builderType The type of the builder, including type parameters
   * @return TypeSpec for the builder class
   */
  private TypeSpec generateBuilderClass(
      TypeElement modelRootType,
      List<ExecutableElement> modelMethods,
      TypeName immutableModelName,
      TypeName immutablePojoName,
      TypeName builderType) {
    ModelRoot modelRoot = modelRootType.getAnnotation(ModelRoot.class);
    // Create fields for the builder class
    List<FieldSpec> fields = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeName fieldType = util.getModelFieldType(method, true, null).fieldType();
      FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, PRIVATE);
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      if (fieldModelRootInfo.isPresent() && !util.isEnumModel(fieldModelRootInfo.get().element())) {
        if (LIST.equals(fieldModelRootInfo.get().containerType())) {
          fieldBuilder.initializer("$T.empty()", ModelsListBuilder.class);
        } else if (ContainerType.MAP.equals(fieldModelRootInfo.get().containerType())) {
          fieldBuilder.initializer("$T.empty()", ModelsMapBuilder.class);
        }
      }
      fields.add(fieldBuilder.build());
    }

    // Create no-arg constructor
    MethodSpec noArgConstructor = MethodSpec.constructorBuilder().addModifiers(PRIVATE).build();

    List<MethodSpec> dataAccessMethods =
        builderGettersAndSetters(modelMethods, builderType, modelRoot, null, util);

    MethodSpec.Builder buildMethodBuilder =
        buildForBuilder(modelMethods, immutablePojoName, util, modelRoot);

    MethodSpec.Builder builderCopyMethodBuilder =
        newCopyForBuilder(modelMethods, builderType, util);

    // Create the builder class
    ClassName builderClassName = asClassName(immutableModelName).nestedClass("Builder");
    return util.classBuilder(
            "Builder",
            modelRootType.getTypeParameters().stream().map(TypeVariableName::get).toList(),
            modelRootType.getQualifiedName().toString())
        .addModifiers(PUBLIC, STATIC, FINAL)
        .addSuperinterface(withTypeParams(builderClassName, modelRootType.getTypeParameters()))
        .addFields(fields)
        .addMethod(noArgConstructor)
        .addMethods(dataAccessMethods)
        .addMethod(buildMethodBuilder.build())
        .addMethod(
            MethodSpec.overriding(util.getMethod(Model.class, "_asBuilder", 0))
                .addModifiers(PUBLIC)
                .returns(builderType)
                .addStatement("return this")
                .build())
        .addMethod(builderCopyMethodBuilder.build())
        .build();
  }

  public static MethodSpec.Builder buildForBuilder(
      List<ExecutableElement> modelMethods,
      TypeName immutablePojoName,
      CodeGenUtility util,
      ModelRoot modelRoot) {
    // Create _build method
    MethodSpec.Builder buildMethodBuilder =
        MethodSpec.methodBuilder("_build")
            .addModifiers(PUBLIC)
            .returns(immutablePojoName)
            .addAnnotation(Override.class);

    // Build the POJO with all fields, with inline validations and defaults
    buildMethodBuilder.addCode("return new $T(", immutablePojoName);
    buildMethodBuilder.addCode(
        modelMethods.stream()
            .map(
                modelMethod ->
                    inlineFieldExpression(modelMethod, immutablePojoName, util, modelRoot))
            .collect(CodeBlock.joining(",")));
    buildMethodBuilder.addCode(");");
    return buildMethodBuilder;
  }

  private static CodeBlock inlineFieldExpression(
      ExecutableElement modelMethod,
      TypeName immutablePojoName,
      CodeGenUtility util,
      ModelRoot modelRoot) {
    String fieldName = modelMethod.getSimpleName().toString();
    CodeBlock fieldAccessorExpr =
        getFieldAccessorExpression(true, fieldName, modelMethod.getReturnType(), modelMethod, util);

    IfAbsentThen ifAbsentThen = util.getIfAbsent(modelMethod, modelRoot).value();
    if (FAIL.equals(ifAbsentThen)) {
      return CodeBlock.of(
          "$T.validateMandatory($L, $S, $T.class)",
          ModelUtils.class,
          fieldAccessorExpr,
          fieldName,
          asClassName(immutablePojoName));
    } else if (ASSUME_DEFAULT_VALUE.equals(ifAbsentThen)) {
      TypeMirror actualType = modelMethod.getReturnType();
      CodeGenType dataType = new DeclaredTypeVisitor(util, modelMethod).visit(actualType, null);
      try {
        CodeBlock defaultExpr = dataType.defaultValueExpr(util.processingEnv());
        return CodeBlock.of(
            "$T.requireNonNullElse($L, $L)",
            ClassName.get(Objects.class),
            fieldAccessorExpr,
            defaultExpr);
      } catch (CodeGenerationException e) {
        throw util.errorAndThrow(
            "Could not find default value expression for type '%s'. Please check if @IfAbsent(ASSUME_DEFAULT_VALUE) is appropriate for this type."
                .formatted(dataType),
            modelMethod);
      }
    } else {
      return fieldAccessorExpr;
    }
  }

  public static MethodSpec.Builder newCopyForBuilder(
      List<ExecutableElement> modelMethods, TypeName builderType, CodeGenUtility util) {
    // Create _newCopy method for the Builder
    MethodSpec.Builder builderCopyMethodBuilder =
        MethodSpec.methodBuilder("_newCopy")
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(builderType);

    // Initialize code to create a new Builder and set all fields
    builderCopyMethodBuilder.addStatement("$T _copy = new $T()", builderType, builderType);
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      if (fieldModelRootInfo.isPresent()
          && !fieldModelRootInfo.get().annotation().builderExtendsModelRoot()
          && !util.isEnumModel(fieldModelRootInfo.get().element())
          && NO_CONTAINER.equals(fieldModelRootInfo.get().containerType())) {
        builderCopyMethodBuilder.addCode(
"""
    if($L instanceof $T _builder) {
      _copy.$L(_builder);
    } else if($L instanceof $T _immut) {
      _copy.$L(_immut);
    }
""",
            fieldName,
            util.getImmutInterfaceName(fieldModelRootInfo.get().element()).nestedClass("Builder"),
            fieldName,
            fieldName,
            util.getImmutInterfaceName(fieldModelRootInfo.get().element()),
            fieldName);
      } else {
        builderCopyMethodBuilder.addStatement(
            "_copy.$L(this.$L)",
            fieldName,
            getFieldAccessorExpression(true, fieldName, method.getReturnType(), method, util));
      }
    }
    builderCopyMethodBuilder.addStatement("return _copy");
    return builderCopyMethodBuilder;
  }

  public static List<MethodSpec> builderGettersAndSetters(
      List<ExecutableElement> modelMethods,
      TypeName builderType,
      ModelRoot modelRoot,
      @Nullable ModelProtocol modelProtocol,
      CodeGenUtility util) {
    // Create setter methods
    List<MethodSpec> dataAccessMethods = new ArrayList<>();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      Optional<ModelRootInfo> fieldModelRootInfo = util.asModelRoot(method.getReturnType(), method);
      dataAccessMethods.add(
          MethodSpec.methodBuilder(methodName)
              .addModifiers(PUBLIC)
              .addParameter(util.getVariableType(method, true), methodName)
              .addAnnotation(Override.class)
              .returns(builderType)
              .addCode(
                  fieldModelRootInfo
                      .filter(info -> !util.isEnumModel(info.element()))
                      .map(ModelRootInfo::containerType)
                      .map(
                          containerType ->
                              switch (containerType) {
                                case NO_CONTAINER ->
                                    CodeBlock.of("this.$L = $L;", methodName, methodName);
                                case LIST, MAP ->
                                    CodeBlock.of(
                                        """
                                            this.$L.clear();
                                            if ($L == null) {
                                                return this;
                                            }
                                            this.$L.$LAllModels($L);
                                        """,
                                        methodName,
                                        methodName,
                                        methodName,
                                        LIST.equals(containerType) ? "add" : "put",
                                        methodName);
                              })
                      .orElse(CodeBlock.of("this.$L = $L;", methodName, methodName)))
              .addStatement("return this")
              .build());

      Optional<ModelRootInfo> fieldModelRoot = util.asModelRoot(method.getReturnType(), method);
      if (fieldModelRoot.isPresent()
          && !fieldModelRoot.get().annotation().builderExtendsModelRoot()
          && !util.isEnumModel(fieldModelRoot.get().element())
          && NO_CONTAINER.equals(fieldModelRoot.get().containerType())) {
        ClassName fieldBuilderType =
            util.getImmutInterfaceName(fieldModelRoot.get().element()).nestedClass("Builder");
        dataAccessMethods.add(
            MethodSpec.methodBuilder(methodName)
                .addModifiers(PUBLIC)
                .addParameter(fieldBuilderType, methodName)
                .addAnnotation(Override.class)
                .returns(builderType)
                .addStatement("this.$L = $L", methodName, methodName)
                .addStatement("return this")
                .build());
      }

      if (modelRoot.builderExtendsModelRoot()
          || (fieldModelRoot.isPresent()
              && !util.isEnumModel(fieldModelRoot.get().element())
              && fieldModelRoot.get().containerType().isContainer())) {
        MethodSpec.Builder getterMethod =
            getterMethod(method, true, modelProtocol, util, builderType, modelRoot)
                .addAnnotation(Override.class);
        dataAccessMethods.add(getterMethod.build());
      }
    }
    return dataAccessMethods;
  }

  /**
   * Validates that REQUEST model fields with @IfAbsent(WILL_NEVER_FAIL) or no @IfAbsent annotation
   * must be marked @Nullable or Optional (excluding lists and maps). For primitive types, they must
   * use @IfAbsent(ASSUME_DEFAULT_VALUE) or be converted to boxed types.
   */
  private void validateOptionalField(ExecutableElement method) {
    IfAbsentThen ifAbsentThen = util.getIfAbsent(method, modelRoot).value();
    ModelRoot modelRoot =
        requireNonNull(codeGenContext.modelRootType().getAnnotation(ModelRoot.class));
    Set<ModelType> types = Set.of(modelRoot.type());
    TypeMirror returnType = method.getReturnType();

    // RESPONSE-only models cannot use MAY_FAIL_CONDITIONALLY
    // (dual-type {REQUEST, RESPONSE} models allow it since they also serve as REQUEST models)
    if (types.contains(ModelType.RESPONSE)
        && !types.contains(ModelType.REQUEST)
        && ifAbsentThen == MAY_FAIL_CONDITIONALLY) {
      util.error(
          "Field '%s' in RESPONSE model '%s' uses @IfAbsent(MAY_FAIL_CONDITIONALLY). "
                  .formatted(
                      method.getSimpleName(), codeGenContext.modelRootType().getQualifiedName())
              + "RESPONSE-only models do not support MAY_FAIL_CONDITIONALLY. "
              + "Use FAIL, WILL_NEVER_FAIL, or ASSUME_DEFAULT_VALUE instead.",
          method);
    }

    // Only validate REQUEST models with WILL_NEVER_FAIL behavior
    if (types.contains(ModelType.REQUEST) && ifAbsentThen == IfAbsentThen.WILL_NEVER_FAIL) {

      // Exclude lists and maps from this validation
      if (util.isListType(returnType) || util.isMapType(returnType)) {
        return;
      }

      // Special validation for primitive types
      if (returnType.getKind().isPrimitive()) {
        util.error(
            "Field '%s' is a primitive type in REQUEST model with @IfAbsent(WILL_NEVER_FAIL). Primitive types must either use @IfAbsent(ASSUME_DEFAULT_VALUE) or be changed to their boxed type (e.g., int -> Integer) with @Nullable or Optional."
                .formatted(method.getSimpleName()),
            method);
        return;
      }

      if (!isMethodOptionalOrNullable(method, util)) {
        util.error(
            "Field '%s' in REQUEST model with @IfAbsent(WILL_NEVER_FAIL) must be Optional or annotated with %s. This ensures application developers handle the null case gracefully."
                .formatted(method.getSimpleName(), Nullable.class.getCanonicalName()),
            method);
      }
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean isMethodOptionalOrNullable(ExecutableElement method, CodeGenUtility util) {
    TypeMirror returnType = method.getReturnType();
    return !returnType.getKind().isPrimitive()
        && (util.isOptional(returnType) || util.isAnyNullable(returnType, method));
  }

  /** Checks if a field has @IfAbsent(FAIL) annotation, meaning it is strictly mandatory. */
  public static boolean isIfAbsentFail(
      ExecutableElement method, CodeGenUtility util, ModelRoot modelRoot) {
    return util.getIfAbsent(method, modelRoot).value() == FAIL;
  }

  /** Checks if an AnnotationSpec represents any @Nullable annotation. */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  static boolean isNullableAnnotation(AnnotationSpec annotationSpec) {
    if (annotationSpec.type instanceof ClassName className) {
      return className.simpleName().equals("Nullable");
    }
    return false;
  }

  /** Strips @Nullable annotations from a TypeName. */
  public static TypeName stripNullableAnnotation(TypeName typeName) {
    List<AnnotationSpec> filtered =
        typeName.annotations.stream().filter(a -> !isNullableAnnotation(a)).toList();
    return typeName.withoutAnnotations().annotated(filtered);
  }
}
