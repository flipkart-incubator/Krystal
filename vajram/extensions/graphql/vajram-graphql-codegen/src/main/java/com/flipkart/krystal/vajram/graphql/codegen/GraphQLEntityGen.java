package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.codegen.common.models.Constants.IMMUT_SUFFIX;
import static com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity.DEFAULT_ENTITY_ID_FIELD;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQLTypeAggregatorGen.GRAPHQL_RESPONSE;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.SchemaReaderUtil.isEntity;
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
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityId;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlEntityModel;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlResponseJson;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlTypeModel;
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

class GraphQLEntityGen implements CodeGenerator {

  private static final String TYPENAME_FIELD = "__typename";
  private final CodeGenUtility util;

  public GraphQLEntityGen(CodeGenUtility util) {
    this.util = util;
  }

  @Override
  public void generate() {
    GraphQlCodeGenUtil graphQlCodeGenUtil = new GraphQlCodeGenUtil(util);
    SchemaReaderUtil schemaReaderUtil = graphQlCodeGenUtil.schemaReaderUtil();
    TypeDefinitionRegistry typeDefinitionRegistry = schemaReaderUtil.typeDefinitionRegistry();
    String rootPackageName = schemaReaderUtil.rootPackageName();

    Map<String, com.squareup.javapoet.TypeName> fieldToClass = new HashMap<>();
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypes = schemaReaderUtil.entityTypes();

    // Write the entity type file
    ClassName entityTypeEnumClassName =
        writeEntityTypeFile(typeDefinitionRegistry.types(), rootPackageName);

    //noinspection rawtypes
    for (Entry<String, TypeDefinition> entry : typeDefinitionRegistry.types().entrySet()) {
      GraphQLTypeName graphQLTypeName = new GraphQLTypeName(entry.getKey());
      @SuppressWarnings("rawtypes")
      TypeDefinition typeDefinition = entry.getValue();
      boolean isEntity = isEntity(typeDefinition);
      ClassName entityClassName = schemaReaderUtil.typeClassName(graphQLTypeName);

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
        GraphQLTypeName enclosingType = GraphQLTypeName.of(typeDefinition);

        boolean idMissing = true;
        if (isEntity) {
          methodSpecs.add(
              MethodSpec.overriding(
                      util.getMethod(
                          () -> GraphQlEntityModel.class.getMethod(DEFAULT_ENTITY_ID_FIELD)))
                  .addModifiers(PUBLIC, ABSTRACT)
                  .returns(schemaReaderUtil.entityIdClassName(entityClassName))
                  .build());
        }
        if (typeDefinition.getChildren() != null) {
          for (int i = 0; i < typeDefinition.getChildren().size(); i++) {
            if (typeDefinition.getChildren().get(i) instanceof FieldDefinition fieldDefinition) {
              String fieldName = fieldDefinition.getName();
              boolean isEntityIdField = DEFAULT_ENTITY_ID_FIELD.equals(fieldName);
              if (isEntity && isEntityIdField) {
                idMissing = false;
                continue;
              }
              GraphQlFieldSpec fieldSpec =
                  schemaReaderUtil.fieldSpecFromField(fieldDefinition, "", enclosingType);
              TypeName typeNameForField = graphQlCodeGenUtil.toTypeNameForField(fieldSpec);
              fieldToClass.put(fieldName, typeNameForField);

              methodSpecs.add(
                  MethodSpec.methodBuilder(fieldName)
                      .addModifiers(PUBLIC, ABSTRACT)
                      .returns(typeNameForField)
                      .build());
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
                        .build())
                .addAnnotation(
                    AnnotationSpec.builder(SupportedModelProtocols.class)
                        .addMember("value", "$T.class", GraphQlResponseJson.class)
                        .build())
                .addModifiers(PUBLIC)
                .addMethods(methodSpecs)
                .addSuperinterface(
                    isEntity
                        ? ParameterizedTypeName.get(
                            ClassName.get(GraphQlEntityModel.class),
                            schemaReaderUtil.entityIdClassName(entityClassName))
                        : ClassName.get(GraphQlTypeModel.class))
                .build();
      } else {
        util.note("Skipping unknown entity type: " + typeDefinition);
        continue;
      }
      util.generateSourceFile(
          entityClassName.canonicalName(),
          JavaFile.builder(entityClassName.packageName(), typeSpec).build().toString(),
          null);
      if (isEntity) {
        var entityIdClassName = schemaReaderUtil.entityIdClassName(entityClassName);
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
    }

    entityTypes.forEach(
        (entity, entityTypeDefinition) -> {
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
          // for dataFetchers which have the size greater than one, create the wrapper class which
          // contains object of those field types
          fieldDefinitions.forEach(
              (dataFetcherName, fieldDefinitionList) -> {
                ClassName className =
                    ClassName.get(
                        dataFetcherName.packageName(),
                        dataFetcherName.simpleName() + GRAPHQL_RESPONSE);
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
                      .addModifiers(PUBLIC, FINAL)
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
      Map<String, TypeDefinition> entityTypes, String rootPackageName) {
    ClassName entityTypeClassName = ClassName.get(rootPackageName, "GraphQLEntityType");
    TypeSpec.Builder entityTypeBuilder = TypeSpec.enumBuilder(entityTypeClassName);
    entityTypeBuilder.addModifiers(PUBLIC);

    for (Entry<String, TypeDefinition> entry : entityTypes.entrySet()) {
      entityTypeBuilder.addEnumConstant(entry.getKey());
    }
    JavaFile javaFileEntityType =
        JavaFile.builder(rootPackageName, entityTypeBuilder.build()).build();

    util.generateSourceFile(
        entityTypeClassName.canonicalName(), javaFileEntityType.toString(), null);
    return entityTypeClassName;
  }
}
