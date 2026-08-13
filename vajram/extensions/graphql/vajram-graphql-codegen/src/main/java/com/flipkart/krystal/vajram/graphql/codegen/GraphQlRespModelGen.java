package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.CodeGenConstants.RESERVED_GRAPHQL_FIELDS_PREFIX;
import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility.ModelRootInfo;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Failure;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlObject;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperation;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponse;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;
import com.squareup.javapoet.WildcardTypeName;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates GraphQL response model implementations (e.g., Order_ImmutGQlRespJson) for entities
 * annotated with @SupportedModelProtocol(GraphQlResponseJson.class).
 *
 * <p>These models wrap field values in {@code Errable<T>} to support partial failures in GraphQL
 * responses, and include GraphQL execution context for proper query resolution.
 */
final class GraphQlRespModelGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;
  private final CodeGenUtility util;

  GraphQlRespModelGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
    this.util = codeGenContext.util();
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    TypeElement modelRootType = codeGenContext.modelRootType();
    CodeGenUtility util = codeGenContext.util();

    ClassName immutClassName = util.getImmutInterfaceName(modelRootType);
    String packageName = immutClassName.packageName();

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.getModelFieldsForCodegen(modelRootType);

    // Generate the GQlRespJson model class
    TypeSpec gqlRespJsonClass =
        generateGQlRespJsonModel(modelRootType, immutClassName, modelMethods);

    // Write the generated class
    util.writeJavaFile(packageName, gqlRespJsonClass, modelRootType);
  }

  /**
   * Generates the complete GQlRespJson model class with: - GraphQL execution context fields -
   * Errable-wrapped fields for all model properties - Constructor with nested entity handling -
   * Builder pattern - Interface method overrides
   */
  private TypeSpec generateGQlRespJsonModel(
      TypeElement modelRootType, ClassName immutClassName, List<ExecutableElement> modelMethods) {
    ClassName gqlRespJsonClassName =
        util.getImmutClassName(modelRootType, GraphQlResponse.INSTANCE);
    boolean hasExecutionContext = hasExecutionContext(modelMethods);

    Builder classBuilder =
        util.classBuilder(
                gqlRespJsonClassName.simpleName(), modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(immutClassName);

    // Add Errable-wrapped fields for all model methods
    addErrableFields(classBuilder, modelMethods);

    // Add constructor
    addConstructor(classBuilder, modelMethods);

    copyCtor(classBuilder, modelRootType, modelMethods);

    // Add interface method overrides
    addInterfaceMethodOverrides(classBuilder, gqlRespJsonClassName, modelMethods);

    // Add __typename method (only for model roots with execution context)
    if (hasExecutionContext) {
      addTypenameMethod(classBuilder, modelRootType);
    }

    // Add static builder method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addStatement("return new Builder()")
            .build());

    // Add Builder class
    addBuilderClass(
        classBuilder, gqlRespJsonClassName, immutClassName, modelMethods, hasExecutionContext);

    return classBuilder.build();
  }

  private void addErrableFields(Builder classBuilder, List<ExecutableElement> modelMethods) {
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      boolean isGraphQLField = methodName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX);

      TypeMirror returnType = method.getReturnType();
      TypeName fieldType;

      // Unwrap outer Errable<T> → T for field storage. The field is always Errable<? extends T>
      // regardless of whether the model method declares T or Errable<T>; the outer Errable
      // on the declared return type signals nullability, not an additional wrapper.
      TypeMirror effectiveReturnType =
          util.isErrable(returnType) ? util.getErrableInnerType(returnType) : returnType;

      // For ALL lists, use nested Errable: Errable<List<Errable<ElementType>>>
      if (util.isListType(effectiveReturnType)) {
        TypeMirror elementType = getListElementType(effectiveReturnType);
        if (elementType != null) {
          // Unwrap element-level Errable<T> → T for the same reason as above.
          TypeMirror effectiveElementType =
              util.isErrable(elementType) ? util.getErrableInnerType(elementType) : elementType;
          TypeName elementTypeName;
          if (util.isModelRoot(effectiveElementType, method)
              && !isEntityIdType(effectiveElementType, util)) {
            // For custom model types (but NOT entity IDs), use _Immut suffix
            TypeElement elementTypeElement =
                (TypeElement)
                    requireNonNull(
                        util.processingEnv().getTypeUtils().asElement(effectiveElementType));
            String packageName =
                util.processingEnv()
                    .getElementUtils()
                    .getPackageOf(elementTypeElement)
                    .getQualifiedName()
                    .toString();
            String simpleName = elementTypeElement.getSimpleName() + "_Immut";
            elementTypeName = ClassName.get(packageName, simpleName);
          } else {
            // For standard types, entity IDs, primitives, etc., use as-is
            elementTypeName = TypeName.get(effectiveElementType);
          }

          // Create List<Errable<ElementType>>
          TypeName innerErrableType =
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName));
          fieldType =
              ParameterizedTypeName.get(
                  ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrableType));
        } else {
          fieldType = TypeName.get(effectiveReturnType);
        }
      } else if (util.isModelRoot(effectiveReturnType)
          && !isEntityIdType(effectiveReturnType, util)) {
        // For single custom model types (but NOT entity IDs), use _Immut suffix
        TypeElement typeElement =
            (TypeElement)
                requireNonNull(util.processingEnv().getTypeUtils().asElement(effectiveReturnType));
        fieldType = util.getImmutInterfaceName(typeElement);
      } else {
        fieldType = TypeName.get(effectiveReturnType);
      }

      TypeName errableFieldType =
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldType));

      classBuilder.addField(
          FieldSpec.builder(
                  isGraphQLField ? fieldType : errableFieldType, methodName, PRIVATE, FINAL)
              .build());
    }
  }

  private void addConstructor(Builder classBuilder, List<ExecutableElement> modelMethods) {

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    // Add parameters for each field (Errable wrapped, except GraphQL context methods)
    for (ExecutableElement method : modelMethods) {
      constructor.addParameter(getConstructorParamType(method), method.getSimpleName().toString());
    }

    // Initialize fields with special handling for nested entities and lists
    // Note: Simple fields are NOT initialized here - they remain as constructor parameters
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();

      boolean isGraphQlField = fieldName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX);

      if (isGraphQlField) {
        constructor.addCode(
"""
    if ($L == null){
      throw new $T("'$L' cannot be null");
    }
""",
            fieldName,
            IllegalArgumentException.class,
            fieldName);
      }

      if (util.isListType(returnType)
          && containsGraphQlModel(getListElementType(returnType), util)) {
        // Handle List<Entity> with complex nested conversion
        addListFieldInitialization(
            constructor, fieldName, returnType, util, ClassName.get(Errable.class));
      } else if (containsGraphQlModel(returnType, util) && !isEntityIdType(returnType, util)) {
        // Handle single Entity with nested conversion (but not entity IDs)
        addEntityFieldInitialization(constructor, fieldName, returnType, util);
      } else {
        // Standard types (String, primitives, List<String>) - ensure non-null initialization
        constructor.addStatement("this.$L = $L", fieldName, fieldName);
      }
    }

    classBuilder.addMethod(constructor.build());
  }

  private TypeName getConstructorParamType(ExecutableElement method) {
    String fieldName = method.getSimpleName().toString();
    boolean isGraphQlField = fieldName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX);

    TypeMirror returnType = method.getReturnType();
    // Unwrap outer Errable<T> → T; the constructor param is Errable<? extends T> either way.
    TypeMirror effectiveReturnType =
        util.isErrable(returnType) ? util.getErrableInnerType(returnType) : returnType;

    TypeName paramInnerType;
    if (util.isListType(effectiveReturnType)) {
      // For ALL lists, parameter type is: Errable<? extends List<Errable<? extends ElementType>>>
      TypeMirror elementType = getListElementType(effectiveReturnType);
      if (elementType != null) {
        // Unwrap element-level Errable<T> → T
        TypeMirror effectiveElementType =
            util.isErrable(elementType) ? util.getErrableInnerType(elementType) : elementType;
        TypeName elementTypeName = TypeName.get(effectiveElementType);
        TypeName innerErrable =
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName));
        // For standard types (String, etc.), use List<Errable<String>> directly (no wildcards)
        paramInnerType =
            ParameterizedTypeName.get(
                ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrable));
      } else {
        paramInnerType = TypeName.get(effectiveReturnType);
      }
    } else {
      paramInnerType = TypeName.get(effectiveReturnType);
    }

    // Determine if we should use wildcards for the Errable wrapper
    TypeName paramType;
    if (isGraphQlField) {
      paramType = paramInnerType;
    } else {
      paramType =
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(paramInnerType));
    }
    return paramType;
  }

  private void copyCtor(
      Builder classBuilder, TypeElement modelRootType, List<ExecutableElement> modelMethods) {

    MethodSpec.Builder constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(util.getImmutClassName(modelRootType, GraphQlResponse.INSTANCE), "_from");
    // Add parameters for each field (Errable wrapped, except GraphQL context methods)
    constructor.addStatement(
        modelMethods.stream()
            .map(method -> CodeBlock.of("_from.$L", method.getSimpleName().toString()))
            .collect(CodeBlock.joining(",", "this(", ")")));
    classBuilder.addMethod(constructor.build());
  }

  private void addListFieldInitialization(
      MethodSpec.Builder constructor,
      String fieldName,
      TypeMirror listType,
      CodeGenUtility util,
      ClassName errableClassName) {

    // Get the element type from List<T>
    TypeMirror elementType = getListElementType(listType);
    if (elementType == null) {
      throw new IllegalStateException("Cannot get element type from list: " + listType);
    }

    // Get the entity type name and its _Immut interface
    TypeName elementTypeName = TypeName.get(elementType);
    TypeElement elementTypeElement =
        (TypeElement) requireNonNull(util.processingEnv().getTypeUtils().asElement(elementType));
    String packageName =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(elementTypeElement)
            .getQualifiedName()
            .toString();
    String simpleName = elementTypeElement.getSimpleName() + "_Immut";
    ClassName immutInterfaceName = ClassName.get(packageName, simpleName);

    constructor.addCode(
        """
        this.$L = $L.mapToValue(
            $T::cast,
            $T::nil,
            _nonNil -> {
              $T<$T<? extends $T>> _immutables = new $T<>(_nonNil.size());
              for ($T<? extends $T> _value : _nonNil) {
                _immutables.add(_value.mapToValue(
                    $T::cast,
                    $T::nil,
                    _nonNil2 ->
                        $T.withValue(
                            _nonNil2
                                ._asBuilder()
                                .graphql_executionContext(graphql_executionContext)
                                .graphql_executionStrategy(graphql_executionStrategy)
                                .graphql_executionStrategyParams(
                                    graphql_executionStrategy.newParametersForFieldExecution(
                                        graphql_executionContext,
                                        graphql_executionStrategyParams,
                                        graphql_executionStrategyParams
                                            .getFields()
                                            .getSubField($S)))
                                ._build())));
              }
              return $T.withValue(_immutables);
            });""",
        fieldName,
        fieldName,
        Failure.class,
        errableClassName,
        List.class,
        Errable.class,
        immutInterfaceName,
        ArrayList.class,
        Errable.class,
        elementTypeName,
        Failure.class,
        errableClassName,
        Errable.class,
        fieldName,
        Errable.class);
  }

  private void addEntityFieldInitialization(
      MethodSpec.Builder constructor,
      String fieldName,
      TypeMirror entityType,
      CodeGenUtility util) {

    // Single entity fields use .mapToValue() with method references
    // Get the raw type name without annotations by extracting the TypeElement
    Element element = util.processingEnv().getTypeUtils().asElement(entityType);
    if (!(element instanceof TypeElement)) {
      throw new IllegalStateException("Cannot get TypeElement for entity type: " + entityType);
    }

    constructor.addCode(
"""
    this.$L = $L.map(
      _nonNil ->
          _nonNil
            ._asBuilder()
            .graphql_executionContext(graphql_executionContext)
            .graphql_executionStrategy(graphql_executionStrategy)
            .graphql_executionStrategyParams(
              graphql_executionStrategy.newParametersForFieldExecution(
                graphql_executionContext,
                graphql_executionStrategyParams,
                graphql_executionStrategyParams
                  .getFields()
                  .getSubField($S)))
            ._build());
""",
        fieldName,
        fieldName,
        fieldName);
  }

  private void addInterfaceMethodOverrides(
      Builder classBuilder, ClassName gqlRespJsonClassName, List<ExecutableElement> modelMethods) {

    // Add _newCopy() method - create new instance with same values
    CodeBlock.Builder newCopyCode =
        CodeBlock.builder()
            .add("return new $T", gqlRespJsonClassName)
            .add(
                modelMethods.stream()
                    .map(method -> CodeBlock.of("\n$L", method.getSimpleName().toString()))
                    .collect(CodeBlock.joining(",", "(", ")")));

    classBuilder.addMethod(
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName)
            .addStatement("$L", newCopyCode.build()) // addStatement adds semicolon
            .build());

    // Add _asBuilder() method - return null (TBD - would require mutable builder)
    ClassName builderType = ClassName.get("", "Builder");
    MethodSpec.Builder asBuilderMethodBuilder =
        MethodSpec.methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(builderType);
    // Initialize code to create a new Builder and set all fields
    asBuilderMethodBuilder.addCode("return new $T()", builderType);
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      Optional<ModelRootInfo> fieldModelRoot = util.asModelRoot(method.getReturnType(), method);
      if (fieldModelRoot.isPresent()) {
        asBuilderMethodBuilder.addCode(
            switch (fieldModelRoot.get().containerType()) {
              case LIST ->
                  """
        .$L($L == null
          ? null
          : $L.map(_list ->
            _list.stream().map(_e ->
              _e == null
                ? null
                : _e.map($T::_asBuilder)).toList()))""";
              default -> ".$L($L == null ? null : $L.map($T::_asBuilder))";
            },
            fieldName,
            fieldName,
            fieldName,
            util.getImmutInterfaceName(fieldModelRoot.get().element()));
      } else {
        // Call the getter method (fieldName()) rather than the field (fieldName) to get the
        // declared return type (exact), not the wildcarded field type.
        asBuilderMethodBuilder.addCode(".$L($L())", fieldName, fieldName);
      }
    }
    asBuilderMethodBuilder.addCode(";");
    classBuilder.addMethod(asBuilderMethodBuilder.build());

    // Add getters for each model method - extract values from Errable fields
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      MethodSpec.Builder getter =
          MethodSpec.methodBuilder(methodName)
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(TypeName.get(method.getReturnType()));

      if (methodName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX)) {
        classBuilder.addMethod(getter.addStatement("return $L", methodName).build());
        continue;
      }

      // Extract actual values from Errable fields (fields are always non-null and final)
      TypeMirror methodReturnType = method.getReturnType();
      if (util.isErrable(methodReturnType)) {
        // Method declares Errable<T>: the stored field IS Errable<? extends T>, return it directly.
        getter
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                    .addMember("value", "$S", "unchecked")
                    .build())
            .addStatement("return ($T) $L", TypeName.get(methodReturnType), methodName);
        classBuilder.addMethod(getter.build());
        continue;
      }
      if (util.isListType(methodReturnType)) {
        // For lists with nested Errable, unwrap both levels using for loop
        // Field is Errable<List<Errable<T_Immut>>>, getter returns List<T> or List<Errable<T>>
        TypeMirror elementType = requireNonNull(getListElementType(methodReturnType));
        // If the declared element type is Errable<T>, the field stores Errable<? extends T>
        // internally. Unwrap to get the actual stored element type.
        boolean elementIsErrable = util.isErrable(elementType);
        TypeMirror effectiveElementType =
            elementIsErrable ? util.getErrableInnerType(elementType) : elementType;
        TypeName elementTypeName = TypeName.get(elementType); // declared type (result list element)

        // Determine the actual type stored in the field (with _Immut for custom models)
        TypeName fieldElementTypeName;
        if (util.isModelRoot(effectiveElementType, method)
            && !isEntityIdType(effectiveElementType, util)) {
          // For custom model types (but NOT entity IDs), field uses _Immut suffix
          TypeElement elementTypeElement =
              (TypeElement)
                  requireNonNull(
                      util.processingEnv().getTypeUtils().asElement(effectiveElementType));
          String packageName =
              util.processingEnv()
                  .getElementUtils()
                  .getPackageOf(elementTypeElement)
                  .getQualifiedName()
                  .toString();
          String simpleName = elementTypeElement.getSimpleName() + "_Immut";
          fieldElementTypeName = ClassName.get(packageName, simpleName);
        } else {
          // For standard types, entity IDs, use effectiveElementType (not the declared Errable<T>)
          fieldElementTypeName = TypeName.get(effectiveElementType);
        }

        getter.addStatement(
            "$T<? extends $T> listOpt = $L.valueOpt().orElse(null)",
            List.class,
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldElementTypeName)),
            methodName);
        // Three branches: model root (needs _Immut cast), Errable elements (keep as Errable),
        // standard scalars (unwrap via valueOpt)
        if (util.isModelRoot(effectiveElementType, method)
            && !isEntityIdType(effectiveElementType, util)) {
          getter.addCode(
              """
              if (listOpt != null) {
                $T<$T> result = new $T<>(listOpt.size());
                for ($T e : listOpt) {
                  result.add(($T) e.valueOpt().orElse(null));
                }
                return result;
              }
              return $T.of();
              """,
              List.class,
              elementTypeName,
              ArrayList.class,
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldElementTypeName)),
              elementTypeName,
              List.class);
        } else if (elementIsErrable) {
          // Declared element type is Errable<T>: stored as Errable<? extends T>, cast directly.
          getter
              .addAnnotation(
                  AnnotationSpec.builder(SuppressWarnings.class)
                      .addMember("value", "$S", "unchecked")
                      .build())
              .addCode(
                  """
                  if (listOpt != null) {
                    $T<$T> result = new $T<>(listOpt.size());
                    for ($T e : listOpt) {
                      result.add(($T) e);
                    }
                    return result;
                  }
                  return $T.of();
                  """,
                  List.class,
                  elementTypeName,
                  ArrayList.class,
                  ParameterizedTypeName.get(
                      ClassName.get(Errable.class),
                      WildcardTypeName.subtypeOf(fieldElementTypeName)),
                  elementTypeName,
                  List.class);
        } else {
          getter.addCode(
              """
              if (listOpt != null) {
                $T<$T> result = new $T<>(listOpt.size());
                for ($T e : listOpt) {
                  result.add(e.valueOpt().orElse(null));
                }
                return result;
              }
              return $T.of();
              """,
              List.class,
              elementTypeName,
              ArrayList.class,
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldElementTypeName)),
              List.class);
        }
      } else if (method.getReturnType().getKind().isPrimitive()) {
        // For primitives, return default if no value
        getter.addStatement(
            "return $L.valueOpt().orElse($L)",
            methodName,
            getPrimitiveDefault(method.getReturnType()));
      } else {
        // For reference types, return null if no value
        getter.addStatement("return $L.valueOpt().orElse(null)", methodName);
      }

      classBuilder.addMethod(getter.build());
    }
  }

  private void addTypenameMethod(Builder classBuilder, TypeElement modelRootType) {
    classBuilder.addMethod(
        MethodSpec.overriding(
                util.getMethod(() -> GraphQlObject.class.getMethod("graphql_typename")))
            .addCode(
                """
                if ($T.isFieldQueried($S, graphql_executionStrategyParams)) {
                  return $T.class.getSimpleName();
                }
                return null;
                """,
                GraphQLUtils.class,
                "__typename",
                TypeName.get(modelRootType.asType()))
            .build());
  }

  private void addBuilderClass(
      Builder parentClassBuilder,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      List<ExecutableElement> modelMethods,
      boolean hasExecutionContext) {

    // Create ClassName constants for frequently used classes
    ClassName failureClassName = ClassName.get("com.flipkart.krystal.data", "Failure");
    ClassName errableClassName = ClassName.get(Errable.class);

    // Determine if this is an entity and get the ID type
    TypeElement modelRootType = codeGenContext.modelRootType();

    Builder builderClass =
        TypeSpec.classBuilder("Builder")
            .addModifiers(PUBLIC, STATIC, FINAL)
            .addSuperinterface(immutClassName.nestedClass("Builder"))
            .addJavadoc("Builder for constructing $T instances.\n", gqlRespJsonClassName);

    // Add GraphQL context fields
    builderClass.addField(ExecutionContext.class, "graphql_executionContext", PRIVATE);
    builderClass.addField(VajramExecutionStrategy.class, "graphql_executionStrategy", PRIVATE);
    builderClass.addField(
        ExecutionStrategyParameters.class, "graphql_executionStrategyParams", PRIVATE);

    // Add Errable-wrapped fields (skip GraphQL context methods)
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL reserved fields
      if (fieldName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX)) {
        continue;
      }

      TypeMirror returnType = method.getReturnType();
      TypeName fieldType =
          util.getModelFieldType(method, true, GraphQlResponse.INSTANCE).fieldType();

      // For ALL lists, use nested Errable: Errable<List<Errable<Element>>>
      // Note: Builder uses non-_Immut types (e.g., Dummy not Dummy_Immut)
      // Note: When element type is Errable<T>, unwrap to T to avoid double-wrapping.
      if (util.isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        if (elementType != null) {
          TypeMirror effectiveElementType =
              util.isErrable(elementType) ? util.getErrableInnerType(elementType) : elementType;
          TypeName elementTypeName = TypeName.get(effectiveElementType);

          // Create List<Errable<Element>>
          TypeName innerErrableType =
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName));
          fieldType =
              ParameterizedTypeName.get(
                  ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrableType));
        }
      }

      // For Errable<T> model methods: use Errable<T> directly as field type (no wildcard wrapping).
      // _Immut.Builder has an abstract setter taking Errable<T> exactly, and _newCopy must call
      // that
      // setter with the stored field value — so the field must be the exact same type.
      // For non-Errable methods: field type is Errable<? extends T> (wildcard, existing behavior).
      TypeName errableFieldType;
      if (util.isErrable(returnType)) {
        errableFieldType = TypeName.get(returnType);
      } else {
        errableFieldType =
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldType));
      }

      builderClass.addField(
          FieldSpec.builder(errableFieldType, fieldName, PRIVATE)
              .initializer("$T.nil()", Errable.class)
              .build());
    }

    // Add GraphQL context setters
    builderClass.addMethod(
        MethodSpec.methodBuilder("graphql_executionContext")
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addParameter(ExecutionContext.class, "graphql_executionContext")
            .addStatement("this.graphql_executionContext = graphql_executionContext")
            .addStatement("return this")
            .build());

    builderClass.addMethod(
        MethodSpec.methodBuilder("graphql_executionStrategy")
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addParameter(VajramExecutionStrategy.class, "graphql_executionStrategy")
            .addStatement("this.graphql_executionStrategy = graphql_executionStrategy")
            .addStatement("return this")
            .build());

    builderClass.addMethod(
        MethodSpec.methodBuilder("graphql_executionStrategyParams")
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams")
            .addStatement("this.graphql_executionStrategyParams = graphql_executionStrategyParams")
            .addStatement("return this")
            .build());

    // Add setters for each field (both direct value and Errable versions)
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - they have dedicated setters above
      if (methodName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX)) {
        continue;
      }

      addBuilderSetters(
          builderClass, method, gqlRespJsonClassName, util, failureClassName, errableClassName);
    }

    // Add _build() method (also adds _newCopy() and id() getter)
    addBuilderBuildMethod(
        builderClass,
        gqlRespJsonClassName,
        modelMethods,
        isGraphQlOpType(modelRootType, util),
        hasExecutionContext);

    parentClassBuilder.addType(builderClass.build());
  }

  private void addBuilderSetters(
      Builder builderClass,
      ExecutableElement method,
      ClassName gqlRespJsonClassName,
      CodeGenUtility util,
      ClassName failureClassName,
      ClassName errableClassName) {

    String fieldName = method.getSimpleName().toString();
    final TypeMirror returnType = method.getReturnType();
    TypeName fieldType = util.getVariableType(method, true);

    TypeMirror listElementType = getListElementType(returnType);
    boolean isListOfEntities = false;

    if (listElementType != null) {
      isListOfEntities =
          util.isModelRoot(listElementType, method) && !isEntityIdType(listElementType, util);
    }

    // Determine the correct Errable field type for the second overload
    TypeName errableFieldType;
    if (listElementType != null) {
      // For lists, use nested Errable structure.
      // If the declared element type is Errable<T>, unwrap to T before building the field type
      // so we don't produce Errable<? extends Errable<T>> (double-wrapped).
      TypeMirror elementType = getListElementType(returnType);
      if (elementType != null) {
        TypeMirror effectiveElementType =
            util.isErrable(elementType) ? util.getErrableInnerType(elementType) : elementType;
        TypeName innerErrableType =
            ParameterizedTypeName.get(
                ClassName.get(Errable.class),
                WildcardTypeName.subtypeOf(TypeName.get(effectiveElementType)));
        TypeName listType =
            ParameterizedTypeName.get(
                ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrableType));
        errableFieldType =
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(listType));
      } else {
        errableFieldType =
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldType));
      }
    } else {
      errableFieldType =
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldType));
    }

    ClassName builderType = gqlRespJsonClassName.nestedClass("Builder");
    // Direct value setter (convenience overload).
    // @Override only when model returns a non-Errable type: _Immut.Builder's abstract setter
    // takes the raw return type (T). When model returns Errable<T>, _Immut.Builder's abstract
    // setter takes Errable<T>, so @Override belongs on the Errable overload instead.
    MethodSpec.Builder directSetterBuilder =
        MethodSpec.methodBuilder(fieldName)
            .addModifiers(PUBLIC)
            .addAnnotation(Override.class)
            .returns(builderType);
    Optional<ModelRootInfo> fieldModelRoot = util.asModelRoot(returnType, method);
    if (listElementType != null) {
      // For ALL lists (entities OR standard types), use complex wrapping logic
      directSetterBuilder.addParameter(fieldType, fieldName);

      // Generate the complex wrapping logic.
      // When the declared element type is already Errable<T>, the parameter is List<Errable<T>>;
      // just wrap the whole list rather than re-wrapping each element.
      TypeMirror elementType = getListElementType(returnType);
      boolean elementIsErrable = util.isErrable(elementType);
      if (elementIsErrable) {
        directSetterBuilder.addCode(
            """
            if ($L == null) {
              this.$L = $T.nil();
              return this;
            }
            this.$L = $T.withValue($L);
            return this;
            """,
            fieldName,
            fieldName,
            Errable.class,
            fieldName,
            Errable.class,
            fieldName);
      } else {
        directSetterBuilder.addCode(
            """
            if ($L == null) {
              this.$L = $T.nil();
              return this;
            }
            $T<$T<$T>> _result = new $T<>($L.size());
            for ($T _item : $L) {
              _result.add($T.withValue(_item));
            }
            this.$L = $T.withValue(_result);
            return this;
            """,
            fieldName,
            fieldName,
            Errable.class,
            List.class,
            Errable.class,
            elementType,
            ArrayList.class,
            fieldName,
            elementType,
            fieldName,
            Errable.class,
            fieldName,
            Errable.class);
      }
    } else if (fieldModelRoot.isPresent()) {
      // Standard scalar: simple wrap
      // For single entity setters, wrap in Errable.withValue()
      ClassName fieldGQlClassName =
          this.util.getImmutClassName(fieldModelRoot.get().element(), GraphQlResponse.INSTANCE);
      directSetterBuilder
          .addParameter(fieldType, fieldName)
          .addCode(
              """
                    if($L instanceof $T || $L instanceof $T){
                      this.$L = $T.withValue($L);
                    } else {
                      throw new $T("Only GQlRespJson or its Builders are supported.");
                    }
              """,
              fieldName,
              fieldGQlClassName.nestedClass("Builder"),
              fieldName,
              fieldGQlClassName,
              fieldName,
              Errable.class,
              fieldName,
              UnsupportedOperationException.class)
          .addStatement("return this");
    } else {
      // Standard scalar: simple wrap
      // For single entity setters, wrap in Errable.withValue()
      directSetterBuilder
          .addParameter(fieldType, fieldName)
          .addStatement("this.$L = $T.withValue($L)", fieldName, Errable.class, fieldName)
          .addStatement("return this");
    }

    MethodSpec directSetter = directSetterBuilder.build();
    builderClass.addMethod(directSetter);

    if (fieldModelRoot.isPresent()
        && !fieldModelRoot.get().annotation().builderExtendsModelRoot()) {

      builderClass.addMethod(
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PUBLIC)
              .addParameter(
                  util.getImmutInterfaceName(fieldModelRoot.get().element()).nestedClass("Builder"),
                  fieldName)
              .addAnnotation(Override.class)
              .returns(builderType)
              .addCode(directSetter.code)
              .build());
    }

    // Errable setter (for aggregator use) - needs to match constructor parameter type
    // For entity lists, use wildcards and .mapToValue() for proper type conversion
    if (listElementType != null && isListOfEntities) {
      // Parameter type with wildcards: Errable<? extends List<? extends Errable<? extends Dummy>>>
      TypeName elementTypeName = TypeName.get(listElementType);

      // Create parameter type with wildcards
      TypeName innerErrableWithWildcard =
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName));
      TypeName listWithWildcard =
          ParameterizedTypeName.get(
              ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrableWithWildcard));
      TypeName errableParamType =
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(listWithWildcard));

      // Generate setter with .mapToValue() to handle type conversion
      MethodSpec.Builder errableSetter =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PUBLIC)
              .returns(builderType)
              .addParameter(
                  ParameterSpec.builder(errableParamType, fieldName)
                      .addAnnotation(Nullable.class)
                      .build());

      // Use .mapToValue() to properly unwrap nested wildcards (similar to constructor logic)
      errableSetter.addCode(
          """
          if ($L == null) {
            this.$L = $T.nil();
            return this;
          }
          this.$L = $L.mapToValue($T::cast, $T::nil, _outerNonNil -> {
            $T<$T> _converted = new $T<>(_outerNonNil.size());
            for ($T _elem : _outerNonNil) {
              _converted.add(_elem.mapToValue($T::cast, $T::nil, _v -> $T.withValue(_v)));
            }
            return $T.withValue(_converted);
          });
          return this;
          """,
          fieldName,
          fieldName,
          Errable.class,
          fieldName,
          fieldName,
          failureClassName,
          errableClassName,
          List.class,
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName)),
          ArrayList.class,
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName)),
          failureClassName,
          errableClassName,
          errableClassName,
          errableClassName);

      builderClass.addMethod(errableSetter.build());
    } else {
      // For non-entity lists and single fields, simple assignment works.
      // For Errable<T> model methods, _Immut.Builder has a second abstract: fieldName(Errable<T>).
      // We must @Override it with the exact type. For non-Errable methods this is a convenience
      // overload using the wildcard errableFieldType.
      MethodSpec.Builder errableSetterBuilder =
          MethodSpec.methodBuilder(fieldName).addModifiers(PUBLIC).returns(builderType);
      if (util.isErrable(returnType)) {
        // _Immut.Builder abstract #2: fieldName(Errable<T>) — use exact type with @Override.
        errableSetterBuilder
            .addAnnotation(Override.class)
            .addParameter(
                ParameterSpec.builder(TypeName.get(returnType), fieldName)
                    .addAnnotation(Nullable.class)
                    .build());
      } else {
        errableSetterBuilder.addParameter(
            ParameterSpec.builder(errableFieldType, fieldName)
                .addAnnotation(Nullable.class)
                .build());
      }
      errableSetterBuilder.addCode(
          """
          if ($L == null) {
            this.$L = $T.nil();
            return this;
          }
          this.$L = $L;
          return this;
          """,
          fieldName,
          fieldName,
          Errable.class,
          fieldName,
          fieldName);
      builderClass.addMethod(errableSetterBuilder.build());
    }
  }

  private void addBuilderBuildMethod(
      Builder builderClass,
      ClassName gqlRespJsonClassName,
      List<ExecutableElement> modelMethods,
      boolean isOpType,
      boolean hasExecutionContext) {

    MethodSpec.Builder buildMethod =
        MethodSpec.methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName);

    // Create new instance with all fields
    CodeBlock.Builder constructorCall =
        CodeBlock.builder().add("return new $T", gqlRespJsonClassName);

    constructorCall.add(
        modelMethods.stream()
            .map(method -> CodeBlock.of("$L", method.getSimpleName().toString()))
            .collect(CodeBlock.joining(",", "(", ")")));

    buildMethod.addStatement("$L", constructorCall.build()); // addStatement adds semicolon

    builderClass.addMethod(buildMethod.build());

    // Add _newCopy() method - creates a new builder with all current values
    MethodSpec.Builder newCopyMethod =
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"));

    // Build the chain of setter calls using hardcoded formatting to avoid JavaPoet indentation
    // issues

    newCopyMethod.addCode(
        """
        return $T._builder()
          .graphql_executionContext(graphql_executionContext)
          .graphql_executionStrategy(graphql_executionStrategy)
          .graphql_executionStrategyParams(graphql_executionStrategyParams)
        """,
        gqlRespJsonClassName);

    // Add model field setters
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - already added above
      if (fieldName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX)) {
        continue;
      }
      newCopyMethod.addCode(".$L($L)", fieldName, fieldName);
    }

    builderClass.addMethod(newCopyMethod.addCode(";").build());

    if (hasExecutionContext) {
      // Add GraphQL execution context getters to Builder (required by GraphQlTypeModel interface)
      builderClass.addMethod(
          MethodSpec.methodBuilder("graphql_executionContext")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(ClassName.get("graphql.execution", "ExecutionContext"))
              .addStatement("return graphql_executionContext")
              .build());

      builderClass.addMethod(
          MethodSpec.methodBuilder("graphql_executionStrategy")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(VajramExecutionStrategy.class)
              .addStatement("return graphql_executionStrategy")
              .build());

      builderClass.addMethod(
          MethodSpec.methodBuilder("graphql_executionStrategyParams")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(ClassName.get("graphql.execution", "ExecutionStrategyParameters"))
              .addStatement("return graphql_executionStrategyParams")
              .build());

      // Add __typename() getter to Builder (required by GraphQlTypeModel interface)
      builderClass.addMethod(
          MethodSpec.overriding(
                  util.getMethod(() -> GraphQlObject.class.getMethod("graphql_typename")))
              .addStatement("return null")
              .build());
    }

    // Add getters for ALL fields in the Builder (required by the Builder interface)
    // The Builder interface extends the model interface, so it must implement all getter methods
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      final TypeMirror returnType = method.getReturnType();

      // Skip GraphQL execution context methods
      if (methodName.startsWith(RESERVED_GRAPHQL_FIELDS_PREFIX)) {
        continue;
      }

      TypeName returnTypeName = TypeName.get(returnType);

      // @Override is valid only when _Immut.Builder extends the model interface
      // (hasExecutionContext
      // model roots). For simple model roots (e.g. _GQlFields), _Immut.Builder only extends
      // ImmutableModel.Builder and does NOT declare model-field getters, so @Override would fail.
      MethodSpec.Builder getter = MethodSpec.methodBuilder(methodName).addModifiers(PUBLIC);
      if (hasExecutionContext) {
        getter.addAnnotation(Override.class);
      }
      getter.returns(returnTypeName);

      // Extract value from Errable field (fields are @NonNull, no null check needed)
      // For lists with nested Errable, unwrap both levels using for loop (same as main class)
      if (util.isErrable(returnType)) {
        // Method declares Errable<T>: the stored field IS exactly Errable<T>, return directly.
        getter.addStatement("return $L", methodName);
        builderClass.addMethod(getter.build());
        continue;
      }
      TypeMirror listElementType = getListElementType(returnType);
      if (listElementType != null) {
        // If the declared element type is Errable<T>, the field stores Errable<? extends T>.
        boolean elementIsErrable = util.isErrable(listElementType);
        TypeMirror effectiveElementType =
            elementIsErrable ? util.getErrableInnerType(listElementType) : listElementType;
        TypeName elementTypeName = TypeName.get(listElementType); // declared element type

        // For Builder fields, use effective (non-Errable) type for stored field element type
        TypeName fieldElementTypeName = TypeName.get(effectiveElementType);

        getter.addStatement(
            "$T<? extends $T> listOpt = $L.valueOpt().orElse(null)",
            List.class,
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldElementTypeName)),
            methodName);
        getter.beginControlFlow("if (listOpt != null)");
        getter.addStatement(
            "$T<$T> result = new $T<>(listOpt.size())",
            List.class,
            elementTypeName,
            ArrayList.class);
        getter.beginControlFlow(
            "for ($T e : listOpt)",
            ParameterizedTypeName.get(
                ClassName.get(Errable.class), WildcardTypeName.subtypeOf(fieldElementTypeName)));
        if (elementIsErrable) {
          // Elements declared as Errable<T>: cast stored Errable<? extends T> to Errable<T>
          getter
              .addAnnotation(
                  AnnotationSpec.builder(SuppressWarnings.class)
                      .addMember("value", "$S", "unchecked")
                      .build())
              .addStatement("result.add(($T) e)", elementTypeName);
        } else {
          getter.addStatement("result.add(e.valueOpt().orElse(null))");
        }
        getter.endControlFlow();
        getter.addStatement("return result");
        getter.endControlFlow();
        getter.addStatement("return $T.of()", List.class);
      } else if (method.getReturnType().getKind().isPrimitive()) {
        getter.addStatement(
            "return $L.valueOpt().orElse($L)",
            methodName,
            getPrimitiveDefault(method.getReturnType()));
      } else {
        getter.addStatement("return $L.valueOpt().orElse(null)", methodName);
      }

      builderClass.addMethod(getter.build());
    }

    // Add stub implementations for GraphQL response methods (_data, _collectErrors, _extensions)
    // These are required by GraphQlTypeModel interface but not used in Builder.
    // Only generate @Override stubs when the model root extends GraphQlObject
    // (hasExecutionContext).
    if (hasExecutionContext) {
      builderClass.addMethod(
          MethodSpec.methodBuilder("_collectErrors")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(void.class)
              .addParameter(ErrorCollector.class, "errorCollector")
              .addParameter(
                  ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(Object.class)),
                  "path")
              .addComment("Stub implementation - Builder doesn't collect errors")
              .build());

      builderClass.addMethod(
          MethodSpec.overriding(util.getMethod(() -> GraphQlObject.class.getMethod("graphql_data")))
              .addStatement("return new $T<>()", LinkedHashMap.class)
              .build());
    }

    if (isOpType) {
      // Add _extensions() method for op types since according to graphql spec only opTypes support
      // extensions
      builderClass.addMethod(
          MethodSpec.overriding(
                  util.getMethod(() -> GraphQlOperation.class.getMethod("graphql_extensions")))
              .addStatement("return null")
              .build());
    }
  }

  // Helper methods

  private boolean isGraphQlOpType(TypeElement modelRootType, CodeGenUtility util) {
    return doesImplementInterface(modelRootType, util, GraphQlOperation.class);
  }

  /**
   * Returns true if the model root has {@code graphql_executionStrategyParams()} as a model method,
   * indicating it carries GraphQL execution context (e.g., entity model roots). Returns false for
   * simple data-container roots such as {@code _GQlFields} interfaces.
   */
  private static boolean hasExecutionContext(List<ExecutableElement> modelMethods) {
    return modelMethods.stream()
        .anyMatch(m -> m.getSimpleName().toString().equals("graphql_executionStrategyParams"));
  }

  @SuppressWarnings("SameParameterValue")
  private static boolean doesImplementInterface(
      TypeElement modelRootType, CodeGenUtility util, Class<?> targetInterface) {
    for (TypeMirror iface : modelRootType.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
      if (ifaceElement != null
          && ifaceElement
              .getQualifiedName()
              .toString()
              .equals(targetInterface.getCanonicalName())) {
        return true;
      }
    }
    return false;
  }

  private @Nullable TypeMirror getListElementType(TypeMirror listType) {
    if (util.isListType(listType) && listType instanceof DeclaredType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) listType).getTypeArguments();
      if (!typeArgs.isEmpty()) {
        return typeArgs.get(0);
      }
    }
    return null;
  }

  /**
   * Checks if the given type is an entity ID type (should NOT get _Immut suffix). Entity IDs
   * typically end with "Id" and are value types, not full entities.
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean isEntityIdType(TypeMirror type, CodeGenUtility util) {
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (!(element instanceof TypeElement)) {
      return false;
    }

    String simpleName = element.getSimpleName().toString();
    // Entity ID types typically end with "Id"
    return simpleName.endsWith("Id");
  }

  private boolean containsGraphQlModel(@Nullable TypeMirror type, CodeGenUtility util) {
    // Check if the type extends GraphQlTypeModel or GraphQlEntityModel
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    TypeElement typeElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(type);
    if (typeElement == null) {
      return false;
    }

    // Check if the type has @SupportedModelProtocol(GraphQlResponseJson.class)
    // This is a more reliable check than interface checking during code generation
    if (util.typeExplicitlySupportsProtocol(typeElement, GraphQlResponse.class)) {
      return true;
    }

    // Fallback: Check interfaces
    for (TypeMirror iface : typeElement.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
      if (ifaceElement != null) {
        String ifaceName = ifaceElement.getQualifiedName().toString();
        if (ifaceName.equals(GraphQlObject.class.getCanonicalName())
            || ifaceName.equals(GraphQlOperation.class.getCanonicalName())) {
          return true;
        }
      }
    }
    return false;
  }

  private String getPrimitiveDefault(TypeMirror type) {
    switch (type.getKind()) {
      case BOOLEAN:
        return "false";
      case BYTE:
      case SHORT:
      case INT:
      case CHAR:
        return "0";
      case LONG:
        return "0L";
      case FLOAT:
        return "0.0f";
      case DOUBLE:
        return "0.0";
      default:
        return "null";
    }
  }

  private boolean isApplicable() {
    // Only run in MODELS phase (same as JsonModelsGen) to avoid conflict with ModelGenProcessor in
    // FINAL phase
    if (!CodegenPhase.MODELS.equals(codeGenContext.codegenPhase())) {
      return false;
    }

    return codeGenContext
        .util()
        .typeExplicitlySupportsProtocol(codeGenContext.modelRootType(), GraphQlResponse.class);
  }
}
