package com.flipkart.krystal.vajram.graphql.codegen;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityModel;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityModel_Immut;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlTypeModel;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates GraphQL response model implementations (e.g., Order_ImmutGQlRespJson) for entities
 * annotated with @SupportedModelProtocols(GraphQlResponseJson.class).
 *
 * <p>These models wrap field values in Errable<T> to support partial failures in GraphQL responses,
 * and include GraphQL execution context for proper query resolution.
 */
public class GraphQlTypeModelGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;

  public GraphQlTypeModelGen(ModelsCodeGenContext codeGenContext) {
    this.codeGenContext = codeGenContext;
  }

  @Override
  public void generate() {
    if (!isApplicable()) {
      return;
    }
    TypeElement modelRootType = codeGenContext.modelRootType();
    CodeGenUtility util = codeGenContext.util();

    ClassName immutClassName = util.getImmutClassName(modelRootType);
    String packageName = immutClassName.packageName();
    ClassName gqlRespJsonClassName =
        ClassName.get(
            packageName,
            immutClassName.simpleName() + GraphQlResponseJson.INSTANCE.modelClassesSuffix());

    // Extract and validate model methods
    List<ExecutableElement> modelMethods = util.extractAndValidateModelMethods(modelRootType);

    // Generate the GQlRespJson model class
    TypeSpec gqlRespJsonClass =
        generateGQlRespJsonModel(
            modelRootType, immutClassName, gqlRespJsonClassName, modelMethods, util);

    // Write the generated class
    util.writeJavaFile(packageName, gqlRespJsonClass, modelRootType);
  }

  /**
   * Generates the complete GQlRespJson model class with: - GraphQL execution context fields -
   * Errable-wrapped fields for all model properties - Constructor with nested entity handling -
   * Builder pattern - Interface method overrides
   */
  private TypeSpec generateGQlRespJsonModel(
      TypeElement modelRootType,
      ClassName immutClassName,
      ClassName gqlRespJsonClassName,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util) {

    boolean isEntity = isGraphQlEntity(modelRootType, util);

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(gqlRespJsonClassName.simpleName())
            .addModifiers(PUBLIC, FINAL)
            .addJavadoc(
                "GraphQL response model for $L.\n\n"
                    + "<p>This class wraps field values in {@link $T} to support partial failures\n"
                    + "in GraphQL responses, allowing individual fields to fail while others succeed.\n"
                    + "\n"
                    + "@see $T\n"
                    + "@generated\n",
                modelRootType.getSimpleName(),
                Errable.class,
                modelRootType.asType());

    // Add interface implementations
    classBuilder.addSuperinterface(immutClassName);
    if (isEntity) {
      // Get the entity ID type from the model
      TypeName entityIdType = getEntityIdType(modelRootType, util);
      classBuilder.addSuperinterface(
          ParameterizedTypeName.get(ClassName.get(GraphQlEntityModel_Immut.class), entityIdType));
    }

    // Add GraphQL execution context fields
    addGraphQLExecutionContextFields(classBuilder);

    // Add Errable-wrapped fields for all model methods
    addErrableFields(classBuilder, modelMethods, util);

    // Add constructor
    addConstructor(classBuilder, gqlRespJsonClassName, modelMethods, util);

    // Add interface method overrides
    addInterfaceMethodOverrides(
        classBuilder, gqlRespJsonClassName, immutClassName, modelMethods, util);

    // Add GraphQL execution context getters
    addGraphQLExecutionContextGetters(classBuilder);

    // Add __typename method
    addTypenameMethod(classBuilder, modelRootType);

    // Add GraphQL response methods (_data, _errors, _extensions)
    addGraphQLResponseMethods(classBuilder);

    // Add static builder method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_builder")
            .addModifiers(PUBLIC, STATIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addStatement("return new Builder()")
            .build());

    // Add Builder class
    addBuilderClass(classBuilder, gqlRespJsonClassName, immutClassName, modelMethods, util);

    return classBuilder.build();
  }

  private void addGraphQLExecutionContextFields(TypeSpec.Builder classBuilder) {
    classBuilder.addField(
        FieldSpec.builder(ExecutionContext.class, "graphql_executionContext", PRIVATE, FINAL)
            .build());
    classBuilder.addField(
        FieldSpec.builder(
                VajramExecutionStrategy.class, "graphql_executionStrategy", PRIVATE, FINAL)
            .build());
    classBuilder.addField(
        FieldSpec.builder(
                ExecutionStrategyParameters.class,
                "graphql_executionStrategyParams",
                PRIVATE,
                FINAL)
            .build());
  }

  private void addErrableFields(
      TypeSpec.Builder classBuilder, List<ExecutableElement> modelMethods, CodeGenUtility util) {
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - they have dedicated final fields
      if (methodName.equals("graphql_executionContext")
          || methodName.equals("graphql_executionStrategy")
          || methodName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      TypeMirror returnType = method.getReturnType();
      TypeName fieldType;

      // For ALL lists, use nested Errable: Errable<List<Errable<ElementType>>>
      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        if (elementType != null) {
          TypeName elementTypeName;
          if (isCustomModelType(elementType, util) && !isEntityIdType(elementType, util)) {
            // For custom model types (but NOT entity IDs), use _Immut suffix
            TypeElement elementTypeElement =
                (TypeElement) util.processingEnv().getTypeUtils().asElement(elementType);
            String elementTypeStr = elementTypeElement.getQualifiedName().toString();
            elementTypeName = ClassName.bestGuess(elementTypeStr + "_Immut");
          } else {
            // For standard types, entity IDs, primitives, etc., use as-is
            elementTypeName = TypeName.get(elementType);
          }

          // Create List<Errable<ElementType>>
          TypeName innerErrableType =
              ParameterizedTypeName.get(ClassName.get(Errable.class), elementTypeName);
          fieldType = ParameterizedTypeName.get(ClassName.get(List.class), innerErrableType);
        } else {
          fieldType = TypeName.get(returnType);
        }
      } else if (isCustomModelType(returnType, util) && !isEntityIdType(returnType, util)) {
        // For single custom model types (but NOT entity IDs), use _Immut suffix
        TypeElement typeElement =
            (TypeElement) util.processingEnv().getTypeUtils().asElement(returnType);
        String typeStr = typeElement.getQualifiedName().toString();
        fieldType = ClassName.bestGuess(typeStr + "_Immut");
      } else {
        fieldType = TypeName.get(returnType);
      }

      TypeName errableFieldType =
          ParameterizedTypeName.get(ClassName.get(Errable.class), fieldType);

      classBuilder.addField(FieldSpec.builder(errableFieldType, methodName, PRIVATE).build());
    }
  }

  private void addConstructor(
      TypeSpec.Builder classBuilder,
      ClassName gqlRespJsonClassName,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util) {

    MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

    // Add GraphQL context parameters
    constructor.addParameter(ExecutionContext.class, "graphql_executionContext");
    constructor.addParameter(VajramExecutionStrategy.class, "graphql_executionStrategy");
    constructor.addParameter(ExecutionStrategyParameters.class, "graphql_executionStrategyParams");

    // Add parameters for each field (Errable wrapped), except GraphQL context methods
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - they're handled above
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      TypeMirror returnType = method.getReturnType();
      TypeName paramInnerType;

      // For ALL lists, parameter type is: Errable<List<Errable<ElementType>>>
      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        if (elementType != null) {
          TypeName elementTypeName;
          boolean isEntityList =
              isCustomModelType(elementType, util) && !isEntityIdType(elementType, util);

          if (isEntityList) {
            // For custom model types (entities), use raw type (not _Immut) with wildcard
            elementTypeName = TypeName.get(elementType);
            // Create: Errable<? extends Entity>
            TypeName innerErrable =
                ParameterizedTypeName.get(
                    ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName));
            // Create: List<? extends Errable<? extends Entity>>
            paramInnerType =
                ParameterizedTypeName.get(
                    ClassName.get(List.class), WildcardTypeName.subtypeOf(innerErrable));
          } else {
            // For standard types (String, etc.), use List<Errable<String>> directly (no wildcards)
            TypeName innerErrable =
                ParameterizedTypeName.get(ClassName.get(Errable.class), TypeName.get(elementType));
            paramInnerType = ParameterizedTypeName.get(ClassName.get(List.class), innerErrable);
          }
        } else {
          paramInnerType = TypeName.get(returnType);
        }
      } else {
        paramInnerType = TypeName.get(returnType);
      }

      // Determine if we should use wildcards for the Errable wrapper
      TypeName paramType;
      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        boolean isEntityList =
            elementType != null
                && isCustomModelType(elementType, util)
                && !isEntityIdType(elementType, util);

        if (isEntityList) {
          // Entity lists: use wildcard wrapper
          paramType =
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(paramInnerType));
        } else {
          // Standard type lists: NO wildcard wrapper
          paramType = ParameterizedTypeName.get(ClassName.get(Errable.class), paramInnerType);
        }
      } else {
        // Single fields: check if custom model type or standard type
        boolean isCustomType = isCustomModelType(returnType, util);

        if (isCustomType) {
          // Custom types (entities, entity IDs): use wildcard wrapper
          paramType =
              ParameterizedTypeName.get(
                  ClassName.get(Errable.class), WildcardTypeName.subtypeOf(paramInnerType));
        } else {
          // Standard types (String, primitives, etc.): NO wildcard wrapper
          paramType = ParameterizedTypeName.get(ClassName.get(Errable.class), paramInnerType);
        }
      }

      constructor.addParameter(paramType, fieldName);
    }

    // Initialize GraphQL context fields
    constructor.addStatement("this.graphql_executionContext = graphql_executionContext");
    constructor.addStatement("this.graphql_executionStrategy = graphql_executionStrategy");
    constructor.addStatement(
        "this.graphql_executionStrategyParams = graphql_executionStrategyParams");

    // Add null check for GraphQL context
    constructor.beginControlFlow(
        "if (graphql_executionContext == null || graphql_executionStrategy == null || graphql_executionStrategyParams == null)");
    constructor.addStatement(
        "throw new $T($S)",
        IllegalArgumentException.class,
        "graphql_executionContext, graphql_executionStrategy or graphql_executionStrategyParams cannot be null");
    constructor.endControlFlow();

    // Initialize fields with special handling for nested entities and lists
    // Note: Simple fields are NOT initialized here - they remain as constructor parameters
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();

      // Skip GraphQL execution context methods - already initialized above
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      if (isListType(returnType) && containsGraphQlModel(getListElementType(returnType), util)) {
        // Handle List<Entity> with complex nested conversion
        addListFieldInitialization(constructor, fieldName, returnType, util);
      } else if (containsGraphQlModel(returnType, util)) {
        // Handle single Entity with nested conversion
        addEntityFieldInitialization(constructor, fieldName, returnType, util);
      }
      // Note: Other fields (non-GraphQL entities) are not initialized -
      // they remain as uninitialized Errable fields and will be set by the builder or aggregator
      // This includes List<String> which has nested Errable but doesn't need complex initialization
    }

    classBuilder.addMethod(constructor.build());
  }

  private void addListFieldInitialization(
      MethodSpec.Builder constructor, String fieldName, TypeMirror listType, CodeGenUtility util) {

    // Get the element type from List<T>
    TypeMirror elementType = getListElementType(listType);
    if (elementType == null) {
      throw new IllegalStateException("Cannot get element type from list: " + listType);
    }

    // Get the entity type name and its _Immut interface
    TypeName elementTypeName = TypeName.get(elementType);
    String elementTypeStr = elementType.toString();
    ClassName immutInterfaceName = ClassName.bestGuess(elementTypeStr + "_Immut");

    // Build the complex .handle() initialization
    // Level 0: Start with fieldName.handle(
    constructor.addCode("$L.handle(\n", fieldName);

    // Level 1: Three lambda parameters for handle()
    constructor.addCode("    _failure -> this.$L = _failure.cast(),\n", fieldName);
    constructor.addCode("    () -> this.$L = $T.nil(),\n", fieldName, Errable.class);
    constructor.addCode("    _nonNil -> {\n");

    // Level 2: Inside _nonNil lambda block
    constructor.addCode(
        "      $T<$T<$T>> _immutables = new $T<>(_nonNil.value().size());\n",
        List.class,
        Errable.class,
        immutInterfaceName,
        java.util.ArrayList.class);
    constructor.addCode(
        "      for ($T<? extends $T> _value : _nonNil.value()) {\n",
        Errable.class,
        elementTypeName);

    // Level 3: Inside for loop - nested _value.handle()
    constructor.addCode("        _value.handle(\n");

    // Level 4: Three lambda parameters for nested handle()
    constructor.addCode("            _failure -> _immutables.add(_failure.cast()),\n");
    constructor.addCode("            () -> _immutables.add($T.nil()),\n", Errable.class);
    constructor.addCode("            _nonNil2 ->\n");

    // Level 5: Multi-line expression for _nonNil2 lambda
    constructor.addCode("                _immutables.add(\n");
    constructor.addCode("                    $T.withValue(\n", Errable.class);
    constructor.addCode("                        _nonNil2\n");
    constructor.addCode("                            .value()\n");
    constructor.addCode("                            ._asBuilder()\n");
    constructor.addCode(
        "                            .graphql_executionContext(graphql_executionContext)\n");
    constructor.addCode(
        "                            .graphql_executionStrategy(graphql_executionStrategy)\n");
    constructor.addCode("                            .graphql_executionStrategyParams(\n");
    constructor.addCode(
        "                                graphql_executionStrategy.newParametersForFieldExecution(\n");
    constructor.addCode("                                    graphql_executionContext,\n");
    constructor.addCode("                                    graphql_executionStrategyParams,\n");
    constructor.addCode("                                    graphql_executionStrategyParams\n");
    constructor.addCode("                                        .getFields()\n");
    constructor.addCode("                                        .getSubField($S)))\n", fieldName);
    constructor.addCode("                            ._build())));\n");

    // Close nested structures
    constructor.addCode("      }\n"); // Close for loop
    constructor.addCode("      this.$L = $T.withValue(_immutables);\n", fieldName, Errable.class);
    constructor.addCode("    });\n"); // Close _nonNil lambda
  }

  private void addEntityFieldInitialization(
      MethodSpec.Builder constructor,
      String fieldName,
      TypeMirror entityType,
      CodeGenUtility util) {

    // Single entity fields also remain uninitialized - will be set by builder/aggregator
    // Note: No initialization needed here, fields remain as Errable parameters
  }

  private void addInterfaceMethodOverrides(
      TypeSpec.Builder classBuilder,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util) {

    // Add _newCopy() method - create new instance with same values
    CodeBlock.Builder newCopyCode = CodeBlock.builder();
    newCopyCode.add("return new $T(\n", gqlRespJsonClassName);
    newCopyCode.indent().indent();
    newCopyCode.add("graphql_executionContext,\n");
    newCopyCode.add("graphql_executionStrategy,\n");
    newCopyCode.add("graphql_executionStrategyParams");

    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();
      if (!fieldName.equals("graphql_executionContext")
          && !fieldName.equals("graphql_executionStrategy")
          && !fieldName.equals("graphql_executionStrategyParams")) {
        newCopyCode.add(",\n$L", fieldName);
      }
    }
    newCopyCode.add(")"); // Close parenthesis
    newCopyCode.unindent().unindent();

    classBuilder.addMethod(
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName)
            .addStatement("$L", newCopyCode.build()) // addStatement adds semicolon
            .build());

    // Add _asBuilder() method - return null (TBD - would require mutable builder)
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_asBuilder")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutClassName.nestedClass("Builder"))
            .addStatement("return null")
            .build());

    // Add getters for each model method - extract values from Errable fields
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      TypeName returnType = TypeName.get(method.getReturnType());

      // Skip GraphQL context methods - they have their own implementations below
      if (methodName.equals("graphql_executionContext")
          || methodName.equals("graphql_executionStrategy")
          || methodName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      MethodSpec.Builder getter =
          MethodSpec.methodBuilder(methodName)
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(returnType);

      // Return stub values (not actual Errable extraction)
      if (isListType(method.getReturnType())) {
        getter.addStatement("return $T.of()", List.class);
      } else if (method.getReturnType().getKind().isPrimitive()) {
        // Return primitive default
        getter.addStatement("return $L", getPrimitiveDefault(method.getReturnType()));
      } else if (isStringType(method.getReturnType(), util)) {
        // String returns empty string
        getter.addStatement("return $S", "");
      } else {
        // Reference types return null
        getter.addStatement("return null");
      }

      classBuilder.addMethod(getter.build());
    }
  }

  private void addGraphQLExecutionContextGetters(TypeSpec.Builder classBuilder) {
    classBuilder.addMethod(
        MethodSpec.methodBuilder("graphql_executionContext")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ExecutionContext.class)
            .addStatement("return graphql_executionContext")
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("graphql_executionStrategy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(VajramExecutionStrategy.class)
            .addStatement("return graphql_executionStrategy")
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("graphql_executionStrategyParams")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(ExecutionStrategyParameters.class)
            .addStatement("return graphql_executionStrategyParams")
            .build());
  }

  private void addTypenameMethod(TypeSpec.Builder classBuilder, TypeElement modelRootType) {
    classBuilder.addMethod(
        MethodSpec.methodBuilder("__typename")
            .addAnnotation(Override.class)
            .addAnnotation(Nullable.class)
            .addModifiers(PUBLIC)
            .returns(String.class)
            .beginControlFlow(
                "if ($T.isFieldQueriedInTheNestedType($S, graphql_executionStrategyParams))",
                ClassName.get("com.flipkart.krystal.vajram.graphql.api", "GraphQLUtils"),
                "__typename")
            .addStatement("return $T.class.getSimpleName()", TypeName.get(modelRootType.asType()))
            .endControlFlow()
            .addStatement("return null")
            .build());
  }

  private void addGraphQLResponseMethods(TypeSpec.Builder classBuilder) {
    // Add _data() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_data")
            .addAnnotation(Override.class)
            .addAnnotation(Nullable.class)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(java.util.Map.class),
                    TypeName.get(String.class),
                    TypeName.get(Object.class)))
            .addComment("TBD")
            .addStatement("throw new $T()", UnsupportedOperationException.class)
            .build());

    // Add _errors() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_errors")
            .addAnnotation(Override.class)
            .addAnnotation(Nullable.class)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(List.class), ClassName.get("graphql", "GraphQLError")))
            .addComment("TBD")
            .addStatement("throw new $T()", UnsupportedOperationException.class)
            .build());

    // Add _extensions() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("_extensions")
            .addAnnotation(Override.class)
            .addAnnotation(Nullable.class)
            .addModifiers(PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(java.util.Map.class),
                    TypeName.get(String.class),
                    TypeName.get(Object.class)))
            .addComment("TBD")
            .addStatement("throw new $T()", UnsupportedOperationException.class)
            .build());
  }

  private void addBuilderClass(
      TypeSpec.Builder parentClassBuilder,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util) {

    // Determine if this is an entity and get the ID type
    TypeElement modelRootType = codeGenContext.modelRootType();
    boolean isEntity = isGraphQlEntity(modelRootType, util);
    TypeName entityIdType =
        isEntity ? getEntityIdType(modelRootType, util) : TypeName.get(String.class);

    TypeSpec.Builder builderClass =
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

      // Skip GraphQL execution context methods
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      TypeMirror returnType = method.getReturnType();
      TypeName fieldType;

      // For ALL lists, use nested Errable: Errable<List<Errable<Entity>>>
      // Note: Builder uses non-_Immut types (e.g., Dummy not Dummy_Immut)
      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        if (elementType != null) {
          // Use the raw element type (no _Immut suffix for builder)
          TypeName elementTypeName = TypeName.get(elementType);

          // Create List<Errable<Entity>>
          TypeName innerErrableType =
              ParameterizedTypeName.get(ClassName.get(Errable.class), elementTypeName);
          fieldType = ParameterizedTypeName.get(ClassName.get(List.class), innerErrableType);
        } else {
          fieldType = TypeName.get(returnType);
        }
      } else {
        fieldType = TypeName.get(returnType);
      }

      TypeName errableFieldType =
          ParameterizedTypeName.get(ClassName.get(Errable.class), fieldType);

      builderClass.addField(errableFieldType, fieldName, PRIVATE);
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
      if (methodName.equals("graphql_executionContext")
          || methodName.equals("graphql_executionStrategy")
          || methodName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      addBuilderSetters(builderClass, method, gqlRespJsonClassName, util);
    }

    // Add _build() method (also adds _newCopy() and id() getter)
    addBuilderBuildMethod(
        builderClass, gqlRespJsonClassName, immutClassName, isEntity, entityIdType, modelMethods);

    parentClassBuilder.addType(builderClass.build());
  }

  private void addBuilderSetters(
      TypeSpec.Builder builderClass,
      ExecutableElement method,
      ClassName gqlRespJsonClassName,
      CodeGenUtility util) {

    String fieldName = method.getSimpleName().toString();
    TypeMirror returnType = method.getReturnType();
    TypeName fieldType = TypeName.get(returnType);

    // Determine if this is a custom entity type (not entity ID)
    boolean isCustomEntity =
        isCustomModelType(returnType, util) && !isEntityIdType(returnType, util);
    boolean isListType = isListType(returnType);
    boolean isListOfEntities = false;

    if (isListType) {
      TypeMirror elementType = getListElementType(returnType);
      isListOfEntities =
          elementType != null
              && isCustomModelType(elementType, util)
              && !isEntityIdType(elementType, util);
    }

    // Determine the correct Errable field type for the second overload
    TypeName errableFieldType;
    if (isListType) {
      // For lists, use nested Errable structure
      TypeMirror elementType = getListElementType(returnType);
      if (elementType != null) {
        TypeName innerErrableType =
            ParameterizedTypeName.get(ClassName.get(Errable.class), TypeName.get(elementType));
        TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), innerErrableType);
        errableFieldType = ParameterizedTypeName.get(ClassName.get(Errable.class), listType);
      } else {
        errableFieldType = ParameterizedTypeName.get(ClassName.get(Errable.class), fieldType);
      }
    } else {
      errableFieldType = ParameterizedTypeName.get(ClassName.get(Errable.class), fieldType);
    }

    // Direct value setter (from interface)
    MethodSpec.Builder directSetter =
        MethodSpec.methodBuilder(fieldName)
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"));

    // For entity setters (single or list), make them stubs
    if (isCustomEntity || isListOfEntities) {
      if (isListType) {
        // List of entities: return null (stub)
        directSetter.addParameter(fieldType, fieldName).addStatement("return null");
      } else {
        // Single entity: just return this (stub)
        directSetter
            .addParameter(
                ParameterSpec.builder(fieldType, fieldName).addAnnotation(Nullable.class).build())
            .addStatement("return this");
      }
    } else if (isListType) {
      // List of standard types (e.g., String): implement complex wrapping logic
      directSetter.addParameter(
          ParameterSpec.builder(fieldType, fieldName).addAnnotation(Nullable.class).build());

      // Generate the complex wrapping logic
      directSetter.beginControlFlow("if ($L == null)", fieldName);
      directSetter.addStatement("this.$L = $T.nil()", fieldName, Errable.class);
      directSetter.addStatement("return this");
      directSetter.endControlFlow();

      directSetter.addStatement(
          "$T<$T<$T>> _result = new $T<>($L.size())",
          List.class,
          Errable.class,
          getListElementType(returnType),
          ArrayList.class,
          fieldName);

      // Get element type name for loop variable
      TypeMirror elementType = getListElementType(returnType);
      String singularName =
          fieldName.endsWith("s")
              ? fieldName.substring(0, fieldName.length() - 1)
              : fieldName + "Item";

      directSetter.beginControlFlow("for ($T $L : $L)", elementType, singularName, fieldName);
      directSetter.addStatement("_result.add($T.withValue($L))", Errable.class, singularName);
      directSetter.endControlFlow();

      directSetter.addStatement("this.$L = $T.withValue(_result)", fieldName, Errable.class);
      directSetter.addStatement("return this");
    } else {
      // Standard scalar: simple wrap
      directSetter
          .addParameter(
              ParameterSpec.builder(fieldType, fieldName).addAnnotation(Nullable.class).build())
          .addStatement("this.$L = $T.withValue($L)", fieldName, Errable.class, fieldName)
          .addStatement("return this");
    }

    builderClass.addMethod(directSetter.build());

    // Errable setter (for aggregator use)
    builderClass.addMethod(
        MethodSpec.methodBuilder(fieldName)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName.nestedClass("Builder"))
            .addParameter(errableFieldType, fieldName)
            .addStatement("this.$L = $L", fieldName, fieldName)
            .addStatement("return this")
            .build());
  }

  private void addBuilderBuildMethod(
      TypeSpec.Builder builderClass,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      boolean isEntity,
      TypeName entityIdType,
      List<ExecutableElement> modelMethods) {

    MethodSpec.Builder buildMethod =
        MethodSpec.methodBuilder("_build")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(gqlRespJsonClassName);

    // Create new instance with all fields
    CodeBlock.Builder constructorCall =
        CodeBlock.builder().add("return new $T(\n", gqlRespJsonClassName);
    constructorCall.indent().indent();
    constructorCall.add("graphql_executionContext,\n");
    constructorCall.add("graphql_executionStrategy,\n");
    constructorCall.add("graphql_executionStrategyParams");

    // Add model fields (skip GraphQL context methods to avoid duplicates)
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - already added above
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      constructorCall.add(",\n$L", fieldName);
    }

    constructorCall.add(")"); // Close parenthesis
    constructorCall.unindent().unindent();

    buildMethod.addStatement("$L", constructorCall.build()); // addStatement adds semicolon

    builderClass.addMethod(buildMethod.build());

    // Add _newCopy() method - creates a new builder with all current values
    MethodSpec.Builder newCopyMethod =
        MethodSpec.methodBuilder("_newCopy")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(immutClassName.nestedClass("Builder"));

    // Build the chain of setter calls using hardcoded formatting to avoid JavaPoet indentation
    // issues
    StringBuilder newCopyCodeStr = new StringBuilder();
    newCopyCodeStr
        .append("return ")
        .append(gqlRespJsonClassName.simpleName())
        .append("._builder()\n");
    newCopyCodeStr.append("    .graphql_executionContext(graphql_executionContext)\n");
    newCopyCodeStr.append("    .graphql_executionStrategy(graphql_executionStrategy)\n");
    newCopyCodeStr.append(
        "    .graphql_executionStrategyParams(graphql_executionStrategyParams)\n");

    // Add model field setters
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL execution context methods - already added above
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      newCopyCodeStr.append("    .").append(fieldName).append("(").append(fieldName).append(")\n");
    }

    // Remove the last newline
    if (newCopyCodeStr.charAt(newCopyCodeStr.length() - 1) == '\n') {
      newCopyCodeStr.setLength(newCopyCodeStr.length() - 1);
    }

    // Use addStatement to automatically add semicolon
    newCopyMethod.addStatement("$L", newCopyCodeStr.toString());
    builderClass.addMethod(newCopyMethod.build());

    // Add id() getter for entities (extracts value from Errable)
    if (isEntity) {
      builderClass.addMethod(
          MethodSpec.methodBuilder("id")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(entityIdType)
              .addStatement("return id.valueOpt().orElse(null)")
              .build());
    }
  }

  private void addBuilderMethods(
      TypeSpec.Builder builderClass,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      boolean isEntity,
      TypeName entityIdType,
      List<ExecutableElement> modelMethods) {
    // This is now integrated into addBuilderBuildMethod above
  }

  // Helper methods

  private boolean isGraphQlEntity(TypeElement modelRootType, CodeGenUtility util) {
    return modelRootType.getInterfaces().stream()
        .anyMatch(
            iface -> {
              TypeElement ifaceElement =
                  (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
              return ifaceElement != null
                  && ifaceElement
                      .getQualifiedName()
                      .toString()
                      .equals(GraphQlEntityModel.class.getCanonicalName());
            });
  }

  private TypeName getEntityIdType(TypeElement modelRootType, CodeGenUtility util) {
    // Extract the generic type parameter from GraphQlEntityModel<T>
    for (TypeMirror iface : modelRootType.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
      if (ifaceElement != null
          && ifaceElement
              .getQualifiedName()
              .toString()
              .equals(GraphQlEntityModel.class.getCanonicalName())) {
        if (iface instanceof DeclaredType) {
          List<? extends TypeMirror> typeArgs = ((DeclaredType) iface).getTypeArguments();
          if (!typeArgs.isEmpty()) {
            return TypeName.get(typeArgs.get(0));
          }
        }
      }
    }
    // Default fallback
    return TypeName.get(String.class);
  }

  private boolean isListType(TypeMirror type) {
    if (type.getKind() != TypeKind.DECLARED) {
      return false;
    }
    DeclaredType declaredType = (DeclaredType) type;
    TypeElement typeElement = (TypeElement) declaredType.asElement();
    return typeElement.getQualifiedName().toString().equals(List.class.getCanonicalName());
  }

  private TypeMirror getListElementType(TypeMirror listType) {
    if (listType instanceof DeclaredType) {
      List<? extends TypeMirror> typeArgs = ((DeclaredType) listType).getTypeArguments();
      if (!typeArgs.isEmpty()) {
        return typeArgs.get(0);
      }
    }
    return null;
  }

  /**
   * Checks if the given type is a custom model type (not String, primitives, or java.* types).
   * Custom model types are user-defined types in the current package or related packages.
   */
  private boolean isCustomModelType(TypeMirror type, CodeGenUtility util) {
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    // Get the raw type without annotations by extracting the TypeElement
    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (!(element instanceof TypeElement)) {
      return false;
    }

    String qualifiedName = ((TypeElement) element).getQualifiedName().toString();
    // Exclude standard Java types
    return !qualifiedName.startsWith("java.") && !qualifiedName.startsWith("javax.");
  }

  /**
   * Checks if the given type is an entity ID type (should NOT get _Immut suffix). Entity IDs
   * typically end with "Id" and are value types, not full entities.
   */
  private boolean isEntityIdType(TypeMirror type, CodeGenUtility util) {
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (!(element instanceof TypeElement)) {
      return false;
    }

    String simpleName = ((TypeElement) element).getSimpleName().toString();
    // Entity ID types typically end with "Id"
    return simpleName.endsWith("Id");
  }

  /** Checks if the given type is String. */
  private boolean isStringType(TypeMirror type, CodeGenUtility util) {
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (!(element instanceof TypeElement)) {
      return false;
    }

    String qualifiedName = ((TypeElement) element).getQualifiedName().toString();
    return qualifiedName.equals("java.lang.String");
  }

  private boolean containsGraphQlModel(TypeMirror type, CodeGenUtility util) {
    // Check if the type extends GraphQlTypeModel or GraphQlEntityModel
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    TypeElement typeElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(type);
    if (typeElement == null) {
      return false;
    }

    // Check if the type has @SupportedModelProtocols(GraphQlResponseJson.class)
    // This is a more reliable check than interface checking during code generation
    SupportedModelProtocols supportedModelProtocols =
        typeElement.getAnnotation(SupportedModelProtocols.class);
    if (supportedModelProtocols != null) {
      if (util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
          .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
          .filter(elem -> elem instanceof QualifiedNameable)
          .map(QualifiedNameable.class::cast)
          .map(element -> requireNonNull(element).getQualifiedName().toString())
          .anyMatch(GraphQlResponseJson.class.getCanonicalName()::equals)) {
        return true;
      }
    }

    // Fallback: Check interfaces
    for (TypeMirror iface : typeElement.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
      if (ifaceElement != null) {
        String ifaceName = ifaceElement.getQualifiedName().toString();
        if (ifaceName.equals(GraphQlTypeModel.class.getCanonicalName())
            || ifaceName.equals(GraphQlEntityModel.class.getCanonicalName())) {
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

    CodeGenUtility util = codeGenContext.util();
    SupportedModelProtocols supportedModelProtocols =
        codeGenContext.modelRootType().getAnnotation(SupportedModelProtocols.class);
    // Check if GraphQlResponseJson is mentioned in the annotation value
    return supportedModelProtocols != null
        && util.getTypesFromAnnotationMember(supportedModelProtocols::value).stream()
            .map(typeMirror -> util.processingEnv().getTypeUtils().asElement(typeMirror))
            .filter(elem -> elem instanceof QualifiedNameable)
            .map(QualifiedNameable.class::cast)
            .map(element -> requireNonNull(element).getQualifiedName().toString())
            .anyMatch(GraphQlResponseJson.class.getCanonicalName()::equals);
  }
}
