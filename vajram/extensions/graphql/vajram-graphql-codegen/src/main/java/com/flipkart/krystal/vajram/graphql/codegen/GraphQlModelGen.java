package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.api.Constants.Directives.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQLObjectAggregateGen.GRAPHQL_RESPONSE;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static java.util.Objects.requireNonNullElse;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityId;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlInputJson;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlObject;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.google.common.collect.Maps;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec.Builder;
import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates Java model interfaces for all GraphQL types (entities, inputs, enums, operation types).
 *
 * <p>This unified generator handles:
 *
 * <ul>
 *   <li>Entity types (ObjectTypeDefinition with @entity directive) - generates response models
 *   <li>Input types (InputObjectTypeDefinition) - generates request models
 *   <li>Enum types (EnumTypeDefinition) - generates enum classes
 *   <li>Operation types (Query, Mutation, Subscription) - generates operation models
 * </ul>
 */
class GraphQlModelGen implements CodeGenerator {

  private final CodeGenUtility util;
  private final GraphQlCodeGenUtil graphQlCodeGenUtil;
  private final SchemaReaderUtil schemaReaderUtil;
  private final Set<String> generatedTypes = new LinkedHashSet<>();

  public GraphQlModelGen(CodeGenUtility util, File schemaFile) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(schemaFile);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  @Override
  public void generate() {
    TypeDefinitionRegistry typeDefinitionRegistry = schemaReaderUtil.typeDefinitionRegistry();
    String rootPackageName = schemaReaderUtil.rootPackageName();

    // Generate input types FIRST (needed by aggregators and vajrams)
    generateInputTypes(typeDefinitionRegistry);

    // Generate entity types, enum types, and operation types
    generateEntityAndEnumTypes(typeDefinitionRegistry);

    // Generate GraphQL field models for multi-field data fetchers
    generateFieldModels(typeDefinitionRegistry);
  }

  /** Generates Java interfaces for all GraphQL input types. */
  private void generateInputTypes(TypeDefinitionRegistry typeDefinitionRegistry) {
    util.note("[GraphQL Model Gen] Starting input type generation");

    List<InputObjectTypeDefinition> inputTypes =
        typeDefinitionRegistry.getTypes(InputObjectTypeDefinition.class);

    if (inputTypes.isEmpty()) {
      util.note("[GraphQL Model Gen] No input types found in schema");
      return;
    }

    util.note("[GraphQL Model Gen] Found " + inputTypes.size() + " input type(s)");

    for (InputObjectTypeDefinition inputType : inputTypes) {
      try {
        generateInputTypeInterface(inputType);
      } catch (Exception e) {
        util.error(
            "[GraphQL Model Gen] Failed to generate input type: "
                + inputType.getName()
                + " - "
                + e.getMessage());
      }
    }

    util.note("[GraphQL Model Gen] Completed input type generation");
  }

  private void generateInputTypeInterface(InputObjectTypeDefinition inputTypeDef) {
    String typeName = inputTypeDef.getName();

    if (generatedTypes.contains(typeName)) {
      return;
    }

    util.note("[GraphQL Model Gen] Generating input type: " + typeName);

    String packageName = schemaReaderUtil.rootPackageName() + ".input";

    // Create interface extending Model with @ModelRoot annotation
    TypeSpec.Builder interfaceBuilder =
        TypeSpec.interfaceBuilder(typeName)
            .addModifiers(PUBLIC)
            .addSuperinterface(Model.class)
            .addAnnotation(
                AnnotationSpec.builder(ModelRoot.class)
                    .addMember("type", "$T.$L", ModelType.class, ModelType.REQUEST)
                    .build())
            .addAnnotation(
                AnnotationSpec.builder(SupportedModelProtocols.class)
                    .addMember(
                        "value",
                        "{$T.class, $T.class}",
                        PlainJavaObject.class,
                        GraphQlInputJson.class)
                    .build())
            .addJavadoc("GraphQL Input Type: {@code $L}\n", typeName);

    if (inputTypeDef.getDescription() != null) {
      String desc = inputTypeDef.getDescription().getContent().trim();
      if (!desc.isEmpty()) {
        interfaceBuilder.addJavadoc("\n<p>$L\n", desc);
      }
    }

    // Add getter method for each field
    for (InputValueDefinition field : inputTypeDef.getInputValueDefinitions()) {
      String fieldName = field.getName();
      com.squareup.javapoet.TypeName fieldType = mapGraphQLType(field.getType());
      boolean isNullable = !(field.getType() instanceof NonNullType);

      MethodSpec.Builder methodBuilder =
          MethodSpec.methodBuilder(fieldName)
              .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
              .returns(fieldType);

      if (field.getDescription() != null) {
        String fieldDesc = field.getDescription().getContent().trim();
        if (!fieldDesc.isEmpty()) {
          methodBuilder.addJavadoc("$L\n", fieldDesc);
        }
      }

      if (isNullable) {
        methodBuilder.addAnnotation(Nullable.class);
      } else {
        // For non-nullable fields, add @IfAbsent(FAIL) to indicate they are mandatory
        methodBuilder.addAnnotation(
            AnnotationSpec.builder(IfAbsent.class)
                .addMember("value", "$T.FAIL", IfAbsentThen.class)
                .build());
      }

      interfaceBuilder.addMethod(methodBuilder.build());
    }

    // Write the file
    try {
      JavaFile javaFile =
          JavaFile.builder(packageName, interfaceBuilder.build()).indent("  ").build();
      String fullName = packageName + "." + typeName;
      util.generateSourceFile(fullName, javaFile.toString(), null);
      generatedTypes.add(typeName);
      util.note("[GraphQL Model Gen] Successfully generated input type: " + typeName);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write file for input type: " + typeName, e);
    }
  }

  /** Maps GraphQL type to Java/JavaPoet TypeName for input types. */
  private com.squareup.javapoet.TypeName mapGraphQLType(graphql.language.Type<?> graphqlType) {
    if (graphqlType instanceof NonNullType nonNull) {
      return mapGraphQLType(nonNull.getType());
    }

    if (graphqlType instanceof ListType listType) {
      com.squareup.javapoet.TypeName elementType = mapGraphQLType(listType.getType());
      return ParameterizedTypeName.get(ClassName.get(List.class), elementType);
    }

    if (graphqlType instanceof graphql.language.TypeName) {
      String typeName = ((graphql.language.TypeName) graphqlType).getName();
      return mapScalarOrInputType(typeName);
    }

    return com.squareup.javapoet.TypeName.OBJECT;
  }

  private com.squareup.javapoet.TypeName mapScalarOrInputType(String typeName) {
    // GraphQL scalar types
    return switch (typeName) {
      case "String", "ID" -> com.squareup.javapoet.TypeName.get(String.class);
      case "Int" -> com.squareup.javapoet.TypeName.get(Integer.class);
      case "Float" -> com.squareup.javapoet.TypeName.get(Float.class);
      case "Boolean" -> com.squareup.javapoet.TypeName.get(Boolean.class);
      default -> {
        // Assume it's another input type in the same package
        String packageName = schemaReaderUtil.rootPackageName() + ".input";
        yield ClassName.get(packageName, typeName);
      }
    };
  }

  /** Generates entity types, enum types, and operation types. */
  private void generateEntityAndEnumTypes(TypeDefinitionRegistry typeDefinitionRegistry) {
    Optional<SchemaDefinition> schemaDefinition = typeDefinitionRegistry.schemaDefinition();
    if (schemaDefinition.isEmpty()) {
      util.note("[GraphQL Model Gen] No schema definition found - skipping entity/enum generation");
      return;
    }
    Map<String, OperationTypeDefinition> opDefsByName =
        schemaDefinition.get().getOperationTypeDefinitions().stream()
            .collect(
                Collectors.toMap(
                    operationTypeDefinition -> operationTypeDefinition.getTypeName().getName(),
                    od -> od));

    Map<String, TypeDefinition> typesWithDataModels =
        Maps.filterValues(
            typeDefinitionRegistry.types(),
            typeDefinition ->
                typeDefinition instanceof EnumTypeDefinition
                    || typeDefinition instanceof ObjectTypeDefinition);
    util.note("[GraphQL Model Gen] Generating models for types: %s".formatted(typesWithDataModels));
    for (Entry<String, TypeDefinition> entry : typesWithDataModels.entrySet()) {
      GraphQLTypeName graphQLTypeName = new GraphQLTypeName(entry.getKey());
      TypeDefinition typeDefinition = entry.getValue();
      try {
        ClassName entityClassName = schemaReaderUtil.typeClassName(graphQLTypeName);

        Builder typeSpec;
        if (typeDefinition instanceof EnumTypeDefinition) {
          Builder enumTypeSpecBuilder =
              TypeSpec.enumBuilder(graphQLTypeName.value()).addModifiers(PUBLIC);

          ((EnumTypeDefinition) typeDefinition)
              .getEnumValueDefinitions()
              .forEach(
                  enumValueDefinition ->
                      enumTypeSpecBuilder.addEnumConstant(enumValueDefinition.getName()));

          typeSpec = enumTypeSpecBuilder;
        } else if (typeDefinition instanceof ObjectTypeDefinition) {
          boolean isEntity = typeDefinition.hasDirective(Directives.ENTITY);
          boolean isOpType = opDefsByName.get(typeDefinition.getName()) != null;
          List<MethodSpec> methodSpecs = new ArrayList<>();
          GraphQLTypeName enclosingType = GraphQLTypeName.of(typeDefinition);

          boolean idMissing = true;
          String entityIdFieldName = schemaReaderUtil.getEntityIdFieldName(typeDefinition);

          if (typeDefinition.getChildren() != null) {
            for (int i = 0; i < typeDefinition.getChildren().size(); i++) {
              if (typeDefinition.getChildren().get(i) instanceof FieldDefinition fieldDefinition) {
                String fieldName = fieldDefinition.getName();
                boolean isEntityIdField = entityIdFieldName.equals(fieldName);

                GraphQlFieldSpec fieldSpec =
                    schemaReaderUtil.fieldSpecFromField(fieldDefinition, "", enclosingType);
                TypeName typeNameForField = graphQlCodeGenUtil.toTypeNameForField(fieldSpec);

                if (isEntity && isEntityIdField) {
                  idMissing = false;
                  typeNameForField = schemaReaderUtil.entityIdClassName(graphQLTypeName);
                }

                methodSpecs.add(
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC, ABSTRACT)
                        .returns(typeNameForField)
                        .addJavadoc(getDescription(fieldDefinition))
                        .build());
              }
            }
            if (isEntity && idMissing) {
              util.error(
                  """
                  The entity %s does not have an '%s' field. Every entity MUST have an id.\
                  Either remove the '@entity' directive from the type or add an '%s' field"""
                      .formatted(
                          entityClassName.simpleName(), entityIdFieldName, entityIdFieldName));
            }
          }

          ClassName immutableClassName =
              ClassName.get(
                  entityClassName.packageName(), entityClassName.simpleName() + "_" + IMMUT_SUFFIX);
          methodSpecs.add(
              MethodSpec.overriding(util.getMethod(() -> Model.class.getMethod("_asBuilder")))
                  .returns(immutableClassName.nestedClass("Builder"))
                  .addModifiers(PUBLIC, ABSTRACT)
                  .build());

          typeSpec =
              util.interfaceBuilder(entityClassName.simpleName(), "")
                  .addAnnotation(
                      AnnotationSpec.builder(ModelRoot.class)
                          .addMember("type", "$T.$L", ModelType.class, ModelType.RESPONSE.name())
                          .addMember("builderExtendsModelRoot", "true")
                          .build())
                  .addAnnotation(
                      AnnotationSpec.builder(SupportedModelProtocols.class)
                          .addMember("value", "$T.class", GraphQlResponseJson.class)
                          .build())
                  .addModifiers(PUBLIC)
                  .addMethods(methodSpecs)
                  .addSuperinterface(
                      isOpType
                          ? ClassName.get(GraphQlOperationObject.class)
                          : ClassName.get(GraphQlObject.class));
          if (isEntity) {
            var entityIdClassName = schemaReaderUtil.entityIdClassName(graphQLTypeName);
            util.generateSourceFile(
                entityIdClassName.canonicalName(),
                JavaFile.builder(
                        entityIdClassName.packageName(),
                        util.classBuilder(
                                entityIdClassName.simpleName(), entityClassName.canonicalName())
                            .addModifiers(PUBLIC)
                            .addSuperinterface(GraphQlEntityId.class)
                            .addField(String.class, "value", PRIVATE, FINAL)
                            .addMethod(
                                MethodSpec.methodBuilder("value")
                                    .addModifiers(PUBLIC)
                                    .returns(String.class)
                                    .addStatement("return value")
                                    .build())
                            .addMethod(
                                MethodSpec.constructorBuilder()
                                    .addModifiers(PUBLIC)
                                    .addParameter(String.class, "value")
                                    .addStatement("this.value = value")
                                    .build())
                            .build())
                    .build()
                    .toString(),
                null);
          }
        } else {
          util.note("[GraphQL Model Gen] Skipping unknown type: " + typeDefinition);
          continue;
        }
        //noinspection ConstantValue
        if (typeDefinition instanceof AbstractDescribedNode<?> describedNode) {
          typeSpec.addJavadoc(getDescription(describedNode));
        }
        util.generateSourceFile(
            entityClassName.canonicalName(),
            JavaFile.builder(entityClassName.packageName(), typeSpec.build()).build().toString(),
            null);
        generatedTypes.add(entityClassName.simpleName());
      } catch (Throwable e) {
        util.error(
            "[GraphQL Model Gen] Could not generate model for type '%s' due to error '%s'"
                .formatted(entry.getKey(), getStackTraceAsString(e)));
      }
    }
  }

  /** Generates GraphQL field models for multi-field data fetchers. */
  private void generateFieldModels(TypeDefinitionRegistry typeDefinitionRegistry) {
    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        schemaReaderUtil.aggregatableTypes();
    util.note(
        "[GraphQL Model Gen] Evaluating '%s' to generate GraphQl Field Models where needed (i.e. if a data fetcher is bound to multiple fields)"
            .formatted(aggregatableTypes));
    aggregatableTypes.forEach(
        (graphQLTypeName, entityTypeDefinition) -> {
          try {
            // Capture all the data fetchers which are mentioned in multiple field definitions

            Map<ClassName, List<FieldDefinition>> fieldDefinitions = new HashMap<>();

            for (FieldDefinition fieldDefinition : entityTypeDefinition.getFieldDefinitions()) {
              if (!fieldDefinition.getDirectives(DATA_FETCHER).isEmpty()) {
                fieldDefinitions
                    .computeIfAbsent(
                        schemaReaderUtil.getDataFetcherClassName(fieldDefinition),
                        _k -> new ArrayList<>())
                    .add(fieldDefinition);
              }
            }
            // for dataFetchers which have the size greater than one, create the wrapper class
            // which
            // contains object of those field types
            fieldDefinitions.forEach(
                (dataFetcherName, fieldDefinitionList) -> {
                  ClassName className =
                      ClassName.get(
                          dataFetcherName.packageName(),
                          dataFetcherName.simpleName() + GRAPHQL_RESPONSE);
                  if (fieldDefinitionList.size() > 1) {
                    Builder builder = TypeSpec.classBuilder(className);

                    for (FieldDefinition fieldDefinitionDf : fieldDefinitionList) {
                      builder.addField(
                          FieldSpec.builder(
                                  graphQlCodeGenUtil.toTypeNameForField(
                                      schemaReaderUtil.fieldSpecFromField(
                                          fieldDefinitionDf, "", graphQLTypeName)),
                                  fieldDefinitionDf.getName(),
                                  PUBLIC)
                              .build());
                    }
                    builder
                        .addModifiers(PUBLIC, FINAL)
                        .addAnnotation(
                            AnnotationSpec.builder(
                                    ClassName.get("lombok.experimental", "Accessors"))
                                .addMember("fluent", "true")
                                .build())
                        .addAnnotation(
                            AnnotationSpec.builder(ClassName.get("lombok", "Getter")).build())
                        .addAnnotation(
                            AnnotationSpec.builder(ClassName.get("lombok", "Builder")).build())
                        .addAnnotation(
                            AnnotationSpec.builder(ClassName.get("lombok", "Setter")).build());
                    util.generateSourceFile(
                        className.canonicalName(),
                        JavaFile.builder(className.packageName(), builder.build())
                            .build()
                            .toString(),
                        null);
                  }
                });
          } catch (Throwable e) {
            util.error(
                "[GraphQL Model Gen] Could not generate GraphQl Fields Models for type '%s' due to error '%s'"
                    .formatted(graphQLTypeName, getStackTraceAsString(e)));
          }
        });
  }

  private static String getDescription(AbstractDescribedNode<?> describedNode) {
    Description description = describedNode.getDescription();
    if (description == null) {
      return "";
    }
    return requireNonNullElse(description.getContent(), "");
  }
}
