package com.flipkart.krystal.vajram.graphql.codegen;

import static java.util.Objects.requireNonNull;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.models.CodegenPhase;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.codegen.common.spi.ModelsCodeGenContext;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.graphql.api.errors.DefaultGraphQLErrorInfo;
import com.flipkart.krystal.vajram.graphql.api.errors.ErrorCollector;
import com.flipkart.krystal.vajram.graphql.api.execution.GraphQLUtils;
import com.flipkart.krystal.vajram.graphql.api.execution.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlObject;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.flipkart.krystal.vajram.graphql.api.model.SerializableGQlResponseJsonModel;
import com.flipkart.krystal.vajram.json.Json;
import com.google.common.base.Suppliers;
import com.squareup.javapoet.ArrayTypeName;
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
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
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
 * <p>These models wrap field values in {@code Errable<T>} to support partial failures in GraphQL
 * responses, and include GraphQL execution context for proper query resolution.
 */
final class GraphQlRespJsonModelGen implements CodeGenerator {

  private final ModelsCodeGenContext codeGenContext;

  GraphQlRespJsonModelGen(ModelsCodeGenContext codeGenContext) {
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

    boolean isOpType = isGraphQlOpType(modelRootType, util);

    TypeSpec.Builder classBuilder =
        util.classBuilder(
                gqlRespJsonClassName.simpleName(), modelRootType.getQualifiedName().toString())
            .addModifiers(PUBLIC, FINAL)
            .addSuperinterface(immutClassName)
            .addSuperinterface(SerializableGQlResponseJsonModel.class);

    classBuilder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(Supplier.class, ObjectReader.class),
                "_READER",
                PRIVATE,
                STATIC,
                FINAL)
            .initializer(
                "$T.memoize(() -> $T.OBJECT_READER.forType($T.class))",
                Suppliers.class,
                Json.class,
                gqlRespJsonClassName)
            .build());
    classBuilder
        .addField(
            FieldSpec.builder(
                    ParameterizedTypeName.get(Supplier.class, ObjectWriter.class),
                    "_WRITER",
                    PRIVATE,
                    STATIC,
                    FINAL)
                .initializer(
                    "$T.memoize(() ->$T.OBJECT_WRITER.forType($T.class))",
                    Suppliers.class,
                    Json.class,
                    gqlRespJsonClassName)
                .build())
        .addField(
            FieldSpec.builder(ArrayTypeName.of(TypeName.BYTE), "_serializedPayload", PRIVATE)
                .addAnnotation(JsonIgnore.class)
                .build());

    // Add GraphQL execution context fields
    addGraphQLExecutionContextFields(classBuilder);

    // Add Errable-wrapped fields for all model methods
    addErrableFields(classBuilder, modelMethods, util);

    // Add constructor
    addConstructor(classBuilder, modelMethods, util);

    // Add interface method overrides
    addInterfaceMethodOverrides(
        classBuilder, gqlRespJsonClassName, immutClassName, modelMethods, util);

    // Add GraphQL execution context getters
    addGraphQLExecutionContextGetters(classBuilder);

    // Add __typename method
    addTypenameMethod(classBuilder, modelRootType);

    // Add GraphQL response methods (_data, _collectErrors, _extensions)
    addGraphQLResponseMethods(classBuilder, modelMethods, util, isOpType);

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
            String packageName =
                util.processingEnv()
                    .getElementUtils()
                    .getPackageOf(elementTypeElement)
                    .getQualifiedName()
                    .toString();
            String simpleName = elementTypeElement.getSimpleName().toString() + "_Immut";
            elementTypeName = ClassName.get(packageName, simpleName);
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
        String packageName =
            util.processingEnv()
                .getElementUtils()
                .getPackageOf(typeElement)
                .getQualifiedName()
                .toString();
        String simpleName = typeElement.getSimpleName().toString() + "_Immut";
        fieldType = ClassName.get(packageName, simpleName);
      } else {
        fieldType = TypeName.get(returnType);
      }

      TypeName errableFieldType =
          ParameterizedTypeName.get(ClassName.get(Errable.class), fieldType);

      classBuilder.addField(
          FieldSpec.builder(errableFieldType, methodName, PRIVATE, FINAL).build());
    }
  }

  private void addConstructor(
      TypeSpec.Builder classBuilder, List<ExecutableElement> modelMethods, CodeGenUtility util) {

    // Create ClassName constants for frequently used classes
    ClassName failureClassName = ClassName.get("com.flipkart.krystal.data", "Failure");
    ClassName errableClassName = ClassName.get(Errable.class);

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
            isCustomModelType(elementType, util) && !isEntityIdType(elementType, util);

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
        addListFieldInitialization(
            constructor, fieldName, returnType, util, failureClassName, errableClassName);
      } else if (containsGraphQlModel(returnType, util) && !isEntityIdType(returnType, util)) {
        // Handle single Entity with nested conversion (but not entity IDs)
        addEntityFieldInitialization(
            constructor, fieldName, returnType, util, failureClassName, errableClassName);
      } else if (isCustomModelType(returnType, util)) {
        // Handle entity IDs and other custom types with wildcards - use .map() with method
        // references
        constructor.addCode(
            "this.$L = $L.map($T::cast, $T::nil, _v -> $T.withValue(_v.value()));\n",
            fieldName,
            fieldName,
            failureClassName,
            errableClassName,
            errableClassName);
      } else {
        // Standard types (String, primitives, List<String>) - ensure non-null initialization
        constructor.addStatement("this.$L = $L", fieldName, fieldName);
      }
    }

    classBuilder.addMethod(constructor.build());
  }

  private void addListFieldInitialization(
      MethodSpec.Builder constructor,
      String fieldName,
      TypeMirror listType,
      CodeGenUtility util,
      ClassName failureClassName,
      ClassName errableClassName) {

    // Get the element type from List<T>
    TypeMirror elementType = getListElementType(listType);
    if (elementType == null) {
      throw new IllegalStateException("Cannot get element type from list: " + listType);
    }

    // Get the entity type name and its _Immut interface
    TypeName elementTypeName = TypeName.get(elementType);
    TypeElement elementTypeElement =
        (TypeElement) util.processingEnv().getTypeUtils().asElement(elementType);
    String packageName =
        util.processingEnv()
            .getElementUtils()
            .getPackageOf(elementTypeElement)
            .getQualifiedName()
            .toString();
    String simpleName = elementTypeElement.getSimpleName().toString() + "_Immut";
    ClassName immutInterfaceName = ClassName.get(packageName, simpleName);

    // Build the .map() initialization with method references (outer map)
    constructor.addCode("this.$L = $L.map($T::cast,\n", fieldName, fieldName, failureClassName);
    constructor.addCode("    $T::nil,\n", errableClassName);
    constructor.addCode("    _nonNil -> {\n");

    // Inside _nonNil lambda block
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

    // Inside for loop - nested _value.map() with method references
    constructor.addCode("        _immutables.add(_value.map(\n");
    constructor.addCode("            $T::cast,\n", failureClassName);
    constructor.addCode("            $T::nil,\n", errableClassName);
    constructor.addCode("            _nonNil2 ->\n");
    constructor.addCode("                $T.withValue(\n", Errable.class);
    constructor.addCode("                    _nonNil2\n");
    constructor.addCode("                        .value()\n");
    constructor.addCode("                        ._asBuilder()\n");
    constructor.addCode(
        "                        .graphql_executionContext(graphql_executionContext)\n");
    constructor.addCode(
        "                        .graphql_executionStrategy(graphql_executionStrategy)\n");
    constructor.addCode("                        .graphql_executionStrategyParams(\n");
    constructor.addCode(
        "                            graphql_executionStrategy.newParametersForFieldExecution(\n");
    constructor.addCode("                                graphql_executionContext,\n");
    constructor.addCode("                                graphql_executionStrategyParams,\n");
    constructor.addCode("                                graphql_executionStrategyParams\n");
    constructor.addCode("                                    .getFields()\n");
    constructor.addCode("                                    .getSubField($S)))\n", fieldName);
    constructor.addCode("                        ._build())));\n");

    // Close structures
    constructor.addCode("      }\n"); // Close for loop
    constructor.addCode("      return $T.withValue(_immutables);\n", Errable.class);
    constructor.addCode("    });\n"); // Close _nonNil lambda
  }

  private void addEntityFieldInitialization(
      MethodSpec.Builder constructor,
      String fieldName,
      TypeMirror entityType,
      CodeGenUtility util,
      ClassName failureClassName,
      ClassName errableClassName) {

    // Single entity fields use .map() with method references
    // Get the raw type name without annotations by extracting the TypeElement
    Element element = util.processingEnv().getTypeUtils().asElement(entityType);
    if (!(element instanceof TypeElement)) {
      throw new IllegalStateException("Cannot get TypeElement for entity type: " + entityType);
    }

    // Build the .map() initialization with method references (matches reference pattern)
    constructor.addCode("this.$L = $L.map($T::cast,\n", fieldName, fieldName, failureClassName);
    constructor.addCode("    $T::nil,\n", errableClassName);
    constructor.addCode("    _nonNil -> $T.withValue(\n", errableClassName);
    constructor.addCode("        _nonNil\n");
    constructor.addCode("            .value()\n");
    constructor.addCode("            ._asBuilder()\n");
    constructor.addCode("            .graphql_executionContext(graphql_executionContext)\n");
    constructor.addCode("            .graphql_executionStrategy(graphql_executionStrategy)\n");
    constructor.addCode("            .graphql_executionStrategyParams(\n");
    constructor.addCode(
        "                graphql_executionStrategy.newParametersForFieldExecution(\n");
    constructor.addCode("                    graphql_executionContext,\n");
    constructor.addCode("                    graphql_executionStrategyParams,\n");
    constructor.addCode("                    graphql_executionStrategyParams\n");
    constructor.addCode("                        .getFields()\n");
    constructor.addCode("                        .getSubField($S)))\n", fieldName);
    constructor.addCode("            ._build()));\n");
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
              .addAnnotation(JsonProperty.class)
              .addModifiers(PUBLIC)
              .returns(returnType);

      // Extract actual values from Errable fields (fields are always non-null and final)
      if (isListType(method.getReturnType())) {
        // For lists with nested Errable, unwrap both levels using for loop
        // Field is Errable<List<Errable<T_Immut>>>, getter returns List<T>
        TypeMirror elementType = getListElementType(method.getReturnType());
        TypeName elementTypeName = TypeName.get(elementType);

        // Determine the actual type stored in the field (with _Immut for custom models)
        TypeName fieldElementTypeName;
        if (isCustomModelType(elementType, util) && !isEntityIdType(elementType, util)) {
          // For custom model types (but NOT entity IDs), field uses _Immut suffix
          TypeElement elementTypeElement =
              (TypeElement) util.processingEnv().getTypeUtils().asElement(elementType);
          String packageName =
              util.processingEnv()
                  .getElementUtils()
                  .getPackageOf(elementTypeElement)
                  .getQualifiedName()
                  .toString();
          String simpleName = elementTypeElement.getSimpleName().toString() + "_Immut";
          fieldElementTypeName = ClassName.get(packageName, simpleName);
        } else {
          // For standard types, entity IDs, use as-is
          fieldElementTypeName = elementTypeName;
        }

        getter.addStatement(
            "$T<$T> listOpt = $L.valueOpt().orElse(null)",
            List.class,
            ParameterizedTypeName.get(ClassName.get(Errable.class), fieldElementTypeName),
            methodName);
        getter.beginControlFlow("if (listOpt != null)");
        getter.addStatement(
            "$T<$T> result = new $T<>(listOpt.size())",
            List.class,
            elementTypeName,
            ArrayList.class);
        getter.beginControlFlow(
            "for ($T e : listOpt)",
            ParameterizedTypeName.get(ClassName.get(Errable.class), fieldElementTypeName));
        // Only cast for custom model types (from _Immut to interface), not for standard types
        if (isCustomModelType(elementType, util) && !isEntityIdType(elementType, util)) {
          getter.addStatement("result.add(($T) e.valueOpt().orElse(null))", elementTypeName);
        } else {
          getter.addStatement("result.add(e.valueOpt().orElse(null))");
        }
        getter.endControlFlow();
        getter.addStatement("return result");
        getter.endControlFlow();
        getter.addStatement("return $T.of()", List.class);
      } else if (method.getReturnType().getKind().isPrimitive()) {
        // For primitives, return default if no value
        getter.addStatement(
            "return $L.valueOpt().orElse($L)",
            methodName,
            getPrimitiveDefault(method.getReturnType()));
      } else if (isStringType(method.getReturnType(), util)) {
        // For String, return empty string if no value
        getter.addStatement("return $L.valueOpt().orElse($S)", methodName, "");
      } else {
        // For reference types, return null if no value
        getter.addStatement("return $L.valueOpt().orElse(null)", methodName);
      }

      classBuilder.addMethod(getter.build());
    }
    // Add _serialize method from Serializable interface with lazy initialization
    classBuilder.addMethod(
        MethodSpec.overriding(util.getMethod(SerializableModel.class, "_serialize", 0))
            .addException(JsonProcessingException.class)
            .addCode(
                """
                if (_serializedPayload == null) {
                  this._serializedPayload = _WRITER.get().writeValueAsBytes(this);
                }
                return _serializedPayload;
                """)
            .build());
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
            .addAnnotation(JsonProperty.class)
            .addModifiers(PUBLIC)
            .returns(String.class)
            .beginControlFlow(
                "if ($T.isFieldQueried($S, graphql_executionStrategyParams))",
                GraphQLUtils.class,
                "__typename")
            .addStatement("return $T.class.getSimpleName()", TypeName.get(modelRootType.asType()))
            .endControlFlow()
            .addStatement("return null")
            .build());
  }

  private void addGraphQLResponseMethods(
      Builder classBuilder,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util,
      boolean isOpType) {

    // Add _collectErrors() method using ErrorCollector pattern
    addCollectErrorsMethod(classBuilder, modelMethods, util);

    if (isOpType) {
      // Add _extensions() method for op types since according to graphql spec only opTypes support
      // extensions
      classBuilder.addMethod(
          MethodSpec.methodBuilder("_extensions")
              .addAnnotation(Override.class)
              .addAnnotation(Nullable.class)
              .addModifiers(PUBLIC)
              .returns(
                  ParameterizedTypeName.get(
                      ClassName.get(java.util.Map.class),
                      ClassName.get(Object.class),
                      TypeName.get(Object.class)))
              .addComment("TBD")
              .addStatement("throw new $T()", UnsupportedOperationException.class)
              .build());
    }
  }

  private void addCollectErrorsMethod(
      TypeSpec.Builder classBuilder, List<ExecutableElement> modelMethods, CodeGenUtility util) {

    MethodSpec.Builder collectErrorsMethod =
        MethodSpec.methodBuilder("_collectErrors")
            .addAnnotation(Override.class)
            .addModifiers(PUBLIC)
            .returns(void.class)
            .addParameter(ErrorCollector.class, "errorCollector")
            .addParameter(ParameterizedTypeName.get(List.class, Object.class), "path")
            .addComment("Collects errors from all Errable fields using the visitor pattern");

    // Collect errors from each field
    for (ExecutableElement method : modelMethods) {
      String fieldName = method.getSimpleName().toString();

      // Skip GraphQL context methods and __typename
      if (fieldName.equals("graphql_executionContext")
          || fieldName.equals("graphql_executionStrategy")
          || fieldName.equals("graphql_executionStrategyParams")
          || fieldName.equals("__typename")) {
        continue;
      }

      TypeMirror returnType = method.getReturnType();

      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        boolean listOfGraphQlModels = containsGraphQlModel(elementType, util);

        // Get the proper TypeName for the element type
        TypeName elementTypeName;
        if (listOfGraphQlModels) {
          // For GraphQL models, use the _Immut interface type
          Element element = util.processingEnv().getTypeUtils().asElement(elementType);
          if (element instanceof TypeElement elementTypeElement) {
            String packageName =
                util.processingEnv()
                    .getElementUtils()
                    .getPackageOf(elementTypeElement)
                    .getQualifiedName()
                    .toString();
            String simpleName = elementTypeElement.getSimpleName().toString() + "_Immut";
            elementTypeName = ClassName.get(packageName, simpleName);
          } else {
            elementTypeName = TypeName.get(elementType);
          }
        } else {
          // For standard types, use the actual type
          elementTypeName = TypeName.get(elementType);
        }

        collectErrorsMethod.addCode("// Collect errors from $L list\n", fieldName);
        collectErrorsMethod.addCode("{\n");
        collectErrorsMethod.addCode(
            "  $T<$T> newPath = new $T<>(path);\n",
            List.class,
            Object.class,
            java.util.ArrayList.class);
        collectErrorsMethod.addCode("  newPath.add($S);\n", fieldName);

        if (listOfGraphQlModels) {
          // For lists of GraphQL models, handle both list-level and element-level errors
          collectErrorsMethod.addCode("  $L.handle(\n", fieldName);
          collectErrorsMethod.addCode("      _failure ->\n");
          collectErrorsMethod.addCode("          errorCollector.addError(\n");
          collectErrorsMethod.addCode(
              "              new $T(newPath, _failure.error())),\n", DefaultGraphQLErrorInfo.class);
          collectErrorsMethod.addCode("      _nonNils -> {\n");
          collectErrorsMethod.addCode(
              "        $T<$T<$T>> _values = _nonNils.value();\n",
              List.class,
              Errable.class,
              elementTypeName);
          collectErrorsMethod.addCode("        for (int i = 0; i < _values.size(); i++) {\n");
          collectErrorsMethod.addCode(
              "          $T<$T> _innerErrable = _values.get(i);\n", Errable.class, elementTypeName);
          collectErrorsMethod.addCode(
              "          $T<$T> innerPath = new $T<>(newPath);\n",
              List.class,
              Object.class,
              java.util.ArrayList.class);
          collectErrorsMethod.addCode("          innerPath.add(i);\n");
          collectErrorsMethod.addCode("          _innerErrable.handle(\n");
          collectErrorsMethod.addCode("              _failure ->\n");
          collectErrorsMethod.addCode("                  errorCollector.addError(\n");
          collectErrorsMethod.addCode(
              "                      new $T(innerPath, _failure.error())),\n",
              DefaultGraphQLErrorInfo.class);
          collectErrorsMethod.addCode(
              "              _nonNil -> _nonNil.value()._collectErrors(errorCollector, innerPath));\n");
          collectErrorsMethod.addCode("        }\n");
          collectErrorsMethod.addCode("      });\n");
        } else {
          // For lists of primitives/standard types
          collectErrorsMethod.addCode("  $L.handle(\n", fieldName);
          collectErrorsMethod.addCode("      _failure ->\n");
          collectErrorsMethod.addCode("          errorCollector.addError(\n");
          collectErrorsMethod.addCode(
              "              new $T(newPath, _failure.error())),\n", DefaultGraphQLErrorInfo.class);
          collectErrorsMethod.addCode("      _nonNils -> {\n");
          collectErrorsMethod.addCode(
              "        $T<$T<$T>> _values = _nonNils.value();\n",
              List.class,
              Errable.class,
              elementTypeName);
          collectErrorsMethod.addCode("        for (int i = 0; i < _values.size(); i++) {\n");
          collectErrorsMethod.addCode("          int index = i;\n");
          collectErrorsMethod.addCode("          _values.get(i).errorOpt().ifPresent(err -> {\n");
          collectErrorsMethod.addCode(
              "            $T<$T> innerPath = new $T<>(newPath);\n",
              List.class,
              Object.class,
              java.util.ArrayList.class);
          collectErrorsMethod.addCode("            innerPath.add(index);\n");
          collectErrorsMethod.addCode("            errorCollector.addError(\n");
          collectErrorsMethod.addCode(
              "                new $T(innerPath, err));\n", DefaultGraphQLErrorInfo.class);
          collectErrorsMethod.addCode("          });\n");
          collectErrorsMethod.addCode("        }\n");
          collectErrorsMethod.addCode("      });\n");
        }

        collectErrorsMethod.addCode("}\n\n");

      } else if (containsGraphQlModel(returnType, util)) {
        // For single GraphQL entity, recursively collect errors
        collectErrorsMethod.addCode("// Collect errors from $L entity\n", fieldName);
        collectErrorsMethod.addCode("{\n");
        collectErrorsMethod.addCode(
            "  $T<$T> newPath = new $T<>(path);\n",
            List.class,
            Object.class,
            java.util.ArrayList.class);
        collectErrorsMethod.addCode("  newPath.add($S);\n", fieldName);
        collectErrorsMethod.addCode("  $L.handle(\n", fieldName);
        collectErrorsMethod.addCode("      _failure ->\n");
        collectErrorsMethod.addCode("          errorCollector.addError(\n");
        collectErrorsMethod.addCode(
            "              new $T(newPath, _failure.error())),\n", DefaultGraphQLErrorInfo.class);
        collectErrorsMethod.addCode(
            "      _nonNil -> _nonNil.value()._collectErrors(errorCollector, newPath));\n");
        collectErrorsMethod.addCode("}\n\n");

      } else {
        // For simple fields (primitives, String, etc.)
        collectErrorsMethod.addCode("// Collect error from $L field\n", fieldName);
        collectErrorsMethod.addCode("$L.errorOpt().ifPresent(err -> {\n", fieldName);
        collectErrorsMethod.addCode(
            "  $T<$T> newPath = new $T<>(path);\n",
            List.class,
            Object.class,
            java.util.ArrayList.class);
        collectErrorsMethod.addCode("  newPath.add($S);\n", fieldName);
        collectErrorsMethod.addCode("  errorCollector.addError(\n");
        collectErrorsMethod.addCode(
            "      new $T(newPath, err));\n", DefaultGraphQLErrorInfo.class);
        collectErrorsMethod.addCode("});\n\n");
      }
    }

    classBuilder.addMethod(collectErrorsMethod.build());
  }

  private void addBuilderClass(
      TypeSpec.Builder parentClassBuilder,
      ClassName gqlRespJsonClassName,
      ClassName immutClassName,
      List<ExecutableElement> modelMethods,
      CodeGenUtility util) {

    // Create ClassName constants for frequently used classes
    ClassName failureClassName = ClassName.get("com.flipkart.krystal.data", "Failure");
    ClassName errableClassName = ClassName.get(Errable.class);

    // Determine if this is an entity and get the ID type
    TypeElement modelRootType = codeGenContext.modelRootType();

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
      if (methodName.equals("graphql_executionContext")
          || methodName.equals("graphql_executionStrategy")
          || methodName.equals("graphql_executionStrategyParams")) {
        continue;
      }

      addBuilderSetters(
          builderClass, method, gqlRespJsonClassName, util, failureClassName, errableClassName);
    }

    // Add _build() method (also adds _newCopy() and id() getter)
    addBuilderBuildMethod(
        builderClass, gqlRespJsonClassName, modelMethods, isGraphQlOpType(modelRootType, util));

    parentClassBuilder.addType(builderClass.build());
  }

  private void addBuilderSetters(
      TypeSpec.Builder builderClass,
      ExecutableElement method,
      ClassName gqlRespJsonClassName,
      CodeGenUtility util,
      ClassName failureClassName,
      ClassName errableClassName) {

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
      isListOfEntities = isCustomModelType(elementType, util) && !isEntityIdType(elementType, util);
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

    // For single entity setters, wrap in Errable.withValue()
    if (isCustomEntity && !isListType) {
      // Single entity: wrap in Errable.withValue()
      directSetter
          .addParameter(
              ParameterSpec.builder(fieldType, fieldName).addAnnotation(Nullable.class).build())
          .addStatement("this.$L = $T.withValue($L)", fieldName, Errable.class, fieldName)
          .addStatement("return this");
    } else if (isListType) {
      // For ALL lists (entities OR standard types), use complex wrapping logic
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
      String singularName = fieldName + "_item";

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

    // Errable setter (for aggregator use) - needs to match constructor parameter type
    // For entity lists, use wildcards and .map() for proper type conversion
    if (isListType && isListOfEntities) {
      // Parameter type with wildcards: Errable<? extends List<? extends Errable<? extends Dummy>>>
      TypeMirror elementType = getListElementType(returnType);
      TypeName elementTypeName = TypeName.get(elementType);

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

      // Generate setter with .map() to handle type conversion
      MethodSpec.Builder errableSetter =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PUBLIC)
              .returns(gqlRespJsonClassName.nestedClass("Builder"))
              .addParameter(
                  ParameterSpec.builder(errableParamType, fieldName)
                      .addAnnotation(Nullable.class)
                      .build());

      errableSetter.beginControlFlow("if ($L == null)", fieldName);
      errableSetter.addStatement("this.$L = $T.nil()", fieldName, Errable.class);
      errableSetter.addStatement("return this");
      errableSetter.endControlFlow();

      // Use .map() to properly unwrap nested wildcards (similar to constructor logic)
      errableSetter.addCode(
          "this.$L = $L.map($T::cast, $T::nil, _outerNonNil -> {\n",
          fieldName,
          fieldName,
          failureClassName,
          errableClassName);
      errableSetter.addCode(
          "  $T<$T> _converted = new $T<>(_outerNonNil.value().size());\n",
          List.class,
          ParameterizedTypeName.get(ClassName.get(Errable.class), elementTypeName),
          ArrayList.class);
      errableSetter.addCode(
          "  for ($T _elem : _outerNonNil.value()) {\n",
          ParameterizedTypeName.get(
              ClassName.get(Errable.class), WildcardTypeName.subtypeOf(elementTypeName)));
      errableSetter.addCode(
          "    _converted.add(_elem.map($T::cast, $T::nil, _v -> $T.withValue(_v.value())));\n",
          failureClassName,
          errableClassName,
          errableClassName);
      errableSetter.addCode("  }\n");
      errableSetter.addCode("  return $T.withValue(_converted);\n", errableClassName);
      errableSetter.addCode("});\n");
      errableSetter.addStatement("return this");

      builderClass.addMethod(errableSetter.build());
    } else {
      // For non-entity lists and single fields, simple assignment works
      builderClass.addMethod(
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(PUBLIC)
              .returns(gqlRespJsonClassName.nestedClass("Builder"))
              .addParameter(
                  ParameterSpec.builder(errableFieldType, fieldName)
                      .addAnnotation(Nullable.class)
                      .build())
              .beginControlFlow("if ($L == null)", fieldName)
              .addStatement("this.$L = $T.nil()", fieldName, Errable.class)
              .addStatement("return this")
              .endControlFlow()
              .addStatement("this.$L = $L", fieldName, fieldName)
              .addStatement("return this")
              .build());
    }
  }

  private void addBuilderBuildMethod(
      Builder builderClass,
      ClassName gqlRespJsonClassName,
      List<ExecutableElement> modelMethods,
      boolean isOpType) {

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
            .returns(gqlRespJsonClassName.nestedClass("Builder"));

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
        MethodSpec.methodBuilder("__typename")
            .addAnnotation(Override.class)
            .addAnnotation(ClassName.get("org.checkerframework.checker.nullness.qual", "Nullable"))
            .addModifiers(PUBLIC)
            .returns(String.class)
            .addStatement("return null")
            .build());

    // Add getters for ALL fields in the Builder (required by the Builder interface)
    // The Builder interface extends the model interface, so it must implement all getter methods
    CodeGenUtility util = codeGenContext.util();
    for (ExecutableElement method : modelMethods) {
      String methodName = method.getSimpleName().toString();
      TypeMirror returnType = method.getReturnType();

      // Skip GraphQL execution context methods and __typename (already added above)
      if (methodName.equals("graphql_executionContext")
          || methodName.equals("graphql_executionStrategy")
          || methodName.equals("graphql_executionStrategyParams")
          || methodName.equals("__typename")) {
        continue;
      }

      TypeName returnTypeName = TypeName.get(returnType);

      MethodSpec.Builder getter =
          MethodSpec.methodBuilder(methodName)
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(returnTypeName);

      // Extract value from Errable field (fields are @NonNull, no null check needed)
      // For lists with nested Errable, unwrap both levels using for loop (same as main class)
      if (isListType(returnType)) {
        TypeMirror elementType = getListElementType(returnType);
        TypeName elementTypeName = TypeName.get(elementType);

        // For Builder fields, use interface types (NOT _Immut) to match field declarations
        // Builder fields are declared as Errable<List<Errable<Dummy>>> (interface type)
        TypeName fieldElementTypeName = elementTypeName;

        getter.addStatement(
            "$T<$T> listOpt = $L.valueOpt().orElse(null)",
            List.class,
            ParameterizedTypeName.get(ClassName.get(Errable.class), fieldElementTypeName),
            methodName);
        getter.beginControlFlow("if (listOpt != null)");
        getter.addStatement(
            "$T<$T> result = new $T<>(listOpt.size())",
            List.class,
            elementTypeName,
            ArrayList.class);
        getter.beginControlFlow(
            "for ($T e : listOpt)",
            ParameterizedTypeName.get(ClassName.get(Errable.class), fieldElementTypeName));
        // No cast needed for Builder getters - fields already use interface types
        getter.addStatement("result.add(e.valueOpt().orElse(null))");
        getter.endControlFlow();
        getter.addStatement("return result");
        getter.endControlFlow();
        getter.addStatement("return $T.of()", List.class);
      } else if (method.getReturnType().getKind().isPrimitive()) {
        getter.addStatement(
            "return $L.valueOpt().orElse($L)",
            methodName,
            getPrimitiveDefault(method.getReturnType()));
      } else if (isStringType(method.getReturnType(), util)) {
        getter.addStatement("return $L.valueOpt().orElse($S)", methodName, "");
      } else {
        getter.addStatement("return $L.valueOpt().orElse(null)", methodName);
      }

      builderClass.addMethod(getter.build());
    }

    // Add stub implementations for GraphQL response methods (_data, _collectErrors, _extensions)
    // These are required by GraphQlTypeModel interface but not used in Builder

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

    if (isOpType) {
      builderClass.addMethod(
          MethodSpec.methodBuilder("_extensions")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .returns(
                  ParameterizedTypeName.get(
                      ClassName.get(Map.class),
                      ClassName.get(Object.class),
                      ClassName.get(Object.class)))
              .addAnnotation(ClassName.get(Nullable.class))
              .addStatement("return null")
              .build());
    }
  }

  // Helper methods

  private boolean isGraphQlOpType(TypeElement modelRootType, CodeGenUtility util) {
    return doesImplementInterface(modelRootType, util, GraphQlOperationObject.class);
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
    if (!(type instanceof DeclaredType declaredType)) {
      return false;
    }
    return util.processingEnv()
        .getTypeUtils()
        .isSubtype(
            type,
            util.processingEnv()
                .getElementUtils()
                .getTypeElement(Model.class.getCanonicalName())
                .asType());
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

  /** Checks if the given type is String. */
  private boolean isStringType(TypeMirror type, CodeGenUtility util) {
    if (type == null || type.getKind() != TypeKind.DECLARED) {
      return false;
    }

    Element element = util.processingEnv().getTypeUtils().asElement(type);
    if (!(element instanceof TypeElement typeElement)) {
      return false;
    }

    String qualifiedName = typeElement.getQualifiedName().toString();
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
      for (TypeMirror typeMirror :
          util.getTypesFromAnnotationMember(supportedModelProtocols::value)) {
        Element elem = util.processingEnv().getTypeUtils().asElement(typeMirror);
        if (elem instanceof QualifiedNameable qualifiedNameable) {
          String qualifiedName = requireNonNull(qualifiedNameable).getQualifiedName().toString();
          if (qualifiedName.equals(GraphQlResponseJson.class.getCanonicalName())) {
            return true;
          }
        }
      }
    }

    // Fallback: Check interfaces
    for (TypeMirror iface : typeElement.getInterfaces()) {
      TypeElement ifaceElement = (TypeElement) util.processingEnv().getTypeUtils().asElement(iface);
      if (ifaceElement != null) {
        String ifaceName = ifaceElement.getQualifiedName().toString();
        if (ifaceName.equals(GraphQlObject.class.getCanonicalName())
            || ifaceName.equals(GraphQlOperationObject.class.getCanonicalName())) {
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
    if (supportedModelProtocols != null) {
      for (TypeMirror typeMirror :
          util.getTypesFromAnnotationMember(supportedModelProtocols::value)) {
        Element elem = util.processingEnv().getTypeUtils().asElement(typeMirror);
        if (elem instanceof QualifiedNameable qualifiedNameable) {
          String qualifiedName = requireNonNull(qualifiedNameable).getQualifiedName().toString();
          if (qualifiedName.equals(GraphQlResponseJson.class.getCanonicalName())) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
