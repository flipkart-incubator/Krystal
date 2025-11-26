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
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.ModelRoot.ModelType;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityId;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;

class GraphQLEntityGen implements CodeGenerator {

  private final CodeGenUtility util;
  private final GraphQlCodeGenUtil graphQlCodeGenUtil;
  private final SchemaReaderUtil schemaReaderUtil;

  public GraphQLEntityGen(CodeGenUtility util, File schemaFile) {
    this.util = util;
    this.graphQlCodeGenUtil = new GraphQlCodeGenUtil(schemaFile);
    this.schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
  }

  @Override
  public void generate() {
    TypeDefinitionRegistry typeDefinitionRegistry = schemaReaderUtil.typeDefinitionRegistry();
    String rootPackageName = schemaReaderUtil.rootPackageName();
    Optional<SchemaDefinition> schemaDefinition = typeDefinitionRegistry.schemaDefinition();
    if (schemaDefinition.isEmpty()) {
      util.note("No schema definition found - skipping entity generation");
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
    util.note("Generating id models and data models for types : %s".formatted(typesWithDataModels));
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
          util.note("Skipping unknown entity type: " + typeDefinition);
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
      } catch (Throwable e) {
        util.error(
            "Could not generate id models and data models for type '%s' due to error '%s'"
                .formatted(entry.getKey(), getStackTraceAsString(e)));
      }
    }

    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        schemaReaderUtil.aggregatableTypes();
    util.note(
        "Evaluating '%s' to generate GraphQl Field Models where needed (i.e. if a data fetcher is bound to multiple fields)"
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
                "Could not generate GraphQl Fields Models for type '%s' due to error '%s'"
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
