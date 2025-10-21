package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity.DEFAULT_ENTITY_ID_FIELD;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQLTypeAggregatorGen.GRAPHQL_RESPONSE;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.GRAPHQL_AGGREGATOR;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.getDirectiveArgumentString;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity;
import com.flipkart.krystal.vajram.graphql.api.AbstractGraphQlModel;
import com.flipkart.krystal.vajram.graphql.api.GraphQLEntityId;
import com.flipkart.krystal.vajram.graphql.codegen.GraphQlFieldSpec.FieldType;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec.Builder;
import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.Modifier;

class GraphQLEntityModelGen implements CodeGenerator {

  private final CodeGenUtility util;

  public GraphQLEntityModelGen(CodeGenUtility util) {
    this.util = util;
  }

  @Override
  public void generate() {
    SchemaReaderUtil schemaReaderUtil = new SchemaReaderUtil(util);
    TypeDefinitionRegistry typeDefinitionRegistry = schemaReaderUtil.typeDefinitionRegistry();
    String rootPackageName = schemaReaderUtil.rootPackageName();

    Map<String, com.squareup.javapoet.TypeName> fieldToClass = new HashMap<>();
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypes =
        schemaReaderUtil.getEntityTypes(typeDefinitionRegistry);

    // Write the entity type file
    ClassName entityTypeEnumClassName = writeEntityTypeFile(entityTypes, rootPackageName);

    //noinspection rawtypes
    for (Entry<String, TypeDefinition> entry : typeDefinitionRegistry.types().entrySet()) {
      GraphQLTypeName graphQLTypeName = new GraphQLTypeName(entry.getKey());
      @SuppressWarnings("rawtypes")
      TypeDefinition typeDefinition = entry.getValue();
      ClassName entityClassName = schemaReaderUtil.entityClassName(graphQLTypeName);
      ClassName entityIdClassName = schemaReaderUtil.entityIdClassName(entityClassName);

      TypeSpec typeSpec;
      if (typeDefinition instanceof EnumTypeDefinition) {
        Builder enumTypeSpecBuilder =
            TypeSpec.enumBuilder(graphQLTypeName.value()).addModifiers(PUBLIC);

        ((EnumTypeDefinition) typeDefinition)
            .getEnumValueDefinitions()
            .forEach(
                enumValueDefinition ->
                    enumTypeSpecBuilder.addEnumConstant(enumValueDefinition.getName()));

        typeSpec = enumTypeSpecBuilder.build();
      } else if (typeDefinition instanceof ObjectTypeDefinition) {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        boolean isEntity = typeDefinition.getDirectivesByName().containsKey("entity");
        String entityIdField =
            getDirectiveArgumentString(typeDefinition, "entity", "identifierKey")
                .orElse(DEFAULT_ENTITY_ID_FIELD);

        boolean idMissing = true;
        if (typeDefinition.getChildren() != null) {
          for (int i = 0; i < typeDefinition.getChildren().size(); i++) {
            if (typeDefinition.getChildren().get(i) instanceof FieldDefinition fieldDefinition) {
              String fieldName = fieldDefinition.getName();
              boolean isEntityIdField = entityIdField.equals(fieldName);
              if (isEntity && isEntityIdField) {
                idMissing = false;
              }
              FieldType fieldType = schemaReaderUtil.getFieldType(fieldDefinition, graphQLTypeName);
              fieldToClass.put(fieldName, fieldType.genericType());

              methodSpecs.add(
                  MethodSpec.methodBuilder(fieldName)
                      .addModifiers(PUBLIC)
                      .returns(fieldType.genericType())
                      .addStatement(
                          "return ($T) _values.get(\"$L\")", fieldType.genericType(), fieldName)
                      .build());
              if (!isEntityIdField) {
                methodSpecs.add(
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC)
                        .addParameter(fieldType.genericType(), "value")
                        .addStatement("_values.put(\"$L\", value)", fieldName)
                        .build());
              }
            }
          }
          if (isEntity && idMissing) {
            util.error(
                """
                The entity %s does not have an 'id' field. Every entity MUST have an id.\
                Either remove the '@entity' directive from the type or add an 'id' field"""
                    .formatted(entityClassName.simpleName()));
          }
        }

        TypeSpec.Builder entityClassBuilder = util.classBuilder(entityClassName.simpleName(), "");
        methodSpecs.add(
            MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameter(entityIdClassName, entityIdField)
                .addStatement("_values.put($S, $L)", entityIdField, entityIdField)
                .build());
        methodSpecs.add(
            MethodSpec.methodBuilder("_new")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(entityClassName)
                .addStatement("return new $T($L())", entityClassName, entityIdField)
                .build());
        methodSpecs.add(
            MethodSpec.methodBuilder("__typename")
                .returns(String.class)
                .addModifiers(PUBLIC)
                .addStatement(
                    "return $T.$L.name()", entityTypeEnumClassName, entityClassName.simpleName())
                .build());
        if (isEntity) {
          typeSpec =
              entityClassBuilder
                  .addModifiers(PUBLIC, FINAL)
                  .addMethods(methodSpecs)
                  .superclass(
                      ParameterizedTypeName.get(
                          ClassName.get(AbstractGraphQLEntity.class),
                          entityIdClassName,
                          entityClassName))
                  .build();
        } else {
          typeSpec =
              entityClassBuilder
                  .addModifiers(PUBLIC, Modifier.FINAL)
                  .addMethods(methodSpecs)
                  .superclass(
                      ParameterizedTypeName.get(
                          ClassName.get(AbstractGraphQlModel.class), entityClassName))
                  .build();
        }
      } else {
        util.note("Skipping unknown entity type: " + typeDefinition);
        continue;
      }
      util.generateSourceFile(
          entityClassName.canonicalName(),
          JavaFile.builder(entityClassName.packageName(), typeSpec).build().toString(),
          null);
      util.generateSourceFile(
          entityIdClassName.canonicalName(),
          JavaFile.builder(
                  entityIdClassName.packageName(),
                  util.classBuilder(entityIdClassName.simpleName(), entityClassName.canonicalName())
                      .addModifiers(PUBLIC)
                      .addSuperinterface(GraphQLEntityId.class)
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

    entityTypes.forEach(
        (entity, entityTypeDefinition) -> {
          // Capture all the data fetchers which are mentioned in multiple field definitions

          Map<String, List<FieldDefinition>> fieldDefinitions = new HashMap<>();

          for (FieldDefinition fieldDefinition : entityTypeDefinition.getFieldDefinitions()) {
            if (!fieldDefinition.getDirectives("dataFetcher").isEmpty()) {
              StringValue stringValue =
                  (StringValue)
                      fieldDefinition
                          .getDirectives("dataFetcher")
                          .get(0)
                          .getArgument("vajramId")
                          .getValue();
              String key = stringValue.getValue();

              if (!fieldDefinitions.containsKey(key)) {
                fieldDefinitions.put(key, new ArrayList<>());
              }

              fieldDefinitions.get(key).add(fieldDefinition);
            }
          }
          // for dataFetchers which have the size greater than one, create the wrapper class which
          // contains object of those field types
          fieldDefinitions.forEach(
              (dataFetcherName, fieldDefinitionList) -> {
                ClassName className =
                    ClassName.get(rootPackageName, dataFetcherName + GRAPHQL_RESPONSE);
                if (fieldDefinitionList.size() > 1) {
                  TypeSpec.Builder builder = TypeSpec.classBuilder(className);

                  for (FieldDefinition fieldDefinitionDf : fieldDefinitionList) {
                    builder.addField(
                        FieldSpec.builder(
                                fieldToClass.get(fieldDefinitionDf.getName()),
                                fieldDefinitionDf.getName(),
                                PUBLIC)
                            .build());
                  }
                  builder
                      .addModifiers(PUBLIC, Modifier.FINAL)
                      .addAnnotation(
                          AnnotationSpec.builder(ClassName.get("lombok.experimental", "Accessors"))
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
                      JavaFile.builder(className.packageName(), builder.build()).build().toString(),
                      null);
                }
              });
        });
  }

  private ClassName writeEntityTypeFile(
      Map<GraphQLTypeName, ObjectTypeDefinition> entityTypes, String rootPackageName) {
    ClassName entityTypeClassName = ClassName.get(rootPackageName, "GraphQLEntityType");
    TypeSpec.Builder entityTypeBuilder = TypeSpec.enumBuilder(entityTypeClassName);
    entityTypeBuilder.addModifiers(PUBLIC);

    for (Entry<GraphQLTypeName, ObjectTypeDefinition> entry : entityTypes.entrySet()) {
      entityTypeBuilder.addEnumConstant(entry.getKey().value());
    }
    JavaFile javaFileEntityType =
        JavaFile.builder(rootPackageName, entityTypeBuilder.build()).build();

    util.generateSourceFile(
        entityTypeClassName.canonicalName(), javaFileEntityType.toString(), null);
    return entityTypeClassName;
  }
}
