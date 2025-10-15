package com.flipkart.krystal.vajram.graphql.codegen;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.flipkart.krystal.vajram.codegen.common.spi.CodeGenerator;
import com.flipkart.krystal.vajram.graphql.api.AbstractGraphQLEntity;
import com.flipkart.krystal.vajram.graphql.api.AbstractGraphQlModel;
import com.flipkart.krystal.vajram.graphql.api.GraphQLEntityId;
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

    // TODO : Write the entity file, currently java poet doesnt support generation of files written
    // in java 17 way like permit, sealed, and recursive generics like Entity<T extends Entity<T>>

    for (Entry<String, TypeDefinition> entry : typeDefinitionRegistry.types().entrySet()) {
      String graphQLTypeName = entry.getKey();
      TypeDefinition typeDefinition = entry.getValue();
      ClassName entityClassName =
          ClassName.get(
              schemaReaderUtil.getPackageNameForType(graphQLTypeName), typeDefinition.getName());
      ClassName entityIdClassName =
          ClassName.get(entityClassName.packageName(), entityClassName.simpleName() + "Id");

      TypeSpec typeSpec;
      if (typeDefinition instanceof EnumTypeDefinition) {
        Builder enumTypeSpecBuilder =
            TypeSpec.enumBuilder(typeDefinition.getName()).addModifiers(PUBLIC);

        ((EnumTypeDefinition) typeDefinition)
            .getEnumValueDefinitions()
            .forEach(
                enumValueDefinition ->
                    enumTypeSpecBuilder.addEnumConstant(enumValueDefinition.getName()));

        typeSpec = enumTypeSpecBuilder.build();
      } else if (typeDefinition instanceof ObjectTypeDefinition) {
        List<MethodSpec> methodSpecs = new ArrayList<>();
        boolean isEntity = typeDefinition.getDirectivesByName().containsKey("entity");
        boolean idMissing = true;
        if (typeDefinition.getChildren() != null) {
          for (int i = 0; i < typeDefinition.getChildren().size(); i++) {
            if (typeDefinition.getChildren().get(i) instanceof FieldDefinition fieldDefinition) {
              String fieldName = fieldDefinition.getName();
              if (isEntity && "id".equals(fieldName)) {
                idMissing = false;
                continue;
              }
              final TypeName fieldTypeName = schemaReaderUtil.getFieldType(fieldDefinition);
              fieldToClass.put(fieldName, fieldTypeName);
              boolean isListType = fieldDefinition.getType() instanceof ListType;

              MethodSpec getterMethodSpec;
              if (isListType) {
                getterMethodSpec =
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC)
                        .returns(fieldTypeName)
                        .addStatement("return ($T) _values.get(\"$L\")", fieldTypeName, fieldName)
                        .build();
              } else {
                getterMethodSpec =
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC)
                        .returns(fieldTypeName)
                        .addStatement("return ($T) _values.get(\"$L\")", fieldTypeName, fieldName)
                        .build();
              }
              MethodSpec setterMethodSpec;
              if (isListType) {
                setterMethodSpec =
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC)
                        .addParameter(ClassName.get(List.class), "value")
                        .addStatement("_values.put(\"$L\", value)", fieldName)
                        .build();
              } else {
                setterMethodSpec =
                    MethodSpec.methodBuilder(fieldName)
                        .addModifiers(PUBLIC)
                        .addParameter(fieldTypeName, "value")
                        .addStatement("_values.put(\"$L\", value)", fieldName)
                        .build();
              }

              methodSpecs.add(getterMethodSpec);
              methodSpecs.add(setterMethodSpec);
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

        TypeSpec.Builder builder = util.classBuilder(entityClassName.simpleName(), "");
        MethodSpec constructor =
            MethodSpec.methodBuilder("_new")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .returns(entityClassName)
                .addStatement("return new $T()", entityClassName)
                .build();
        methodSpecs.add(constructor);
        methodSpecs.add(
            MethodSpec.methodBuilder("__typename")
                .returns(String.class)
                .addModifiers(PUBLIC)
                .addStatement(
                    "return $T.$L.name()", entityTypeEnumClassName, entityClassName.simpleName())
                .build());
        if (isEntity) {
          typeSpec =
              builder
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
              builder
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
                      .addField(String.class, "id", PRIVATE, FINAL)
                      .addMethod(
                          MethodSpec.methodBuilder("id")
                              .addModifiers(PUBLIC)
                              .returns(String.class)
                              .addStatement("return id")
                              .build())
                      .addMethod(
                          MethodSpec.constructorBuilder()
                              .addModifiers(PUBLIC)
                              .addParameter(String.class, "id")
                              .addStatement("this.id = id")
                              .build())
                      .build())
              .build()
              .toString(),
          null);
    }

    entityTypes.forEach(
        (entity, entityTypeDefinition) -> {
          ObjectTypeDefinition objectTypeDefinition = entityTypeDefinition;
          // Capture all the data fetchers which are mentioned in multiple field definitions

          Map<String, List<FieldDefinition>> fieldDefinitions = new HashMap<>();

          for (FieldDefinition fieldDefinition : objectTypeDefinition.getFieldDefinitions()) {
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
                    ClassName.get(rootPackageName, dataFetcherName + "GraphQLResponse");
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
