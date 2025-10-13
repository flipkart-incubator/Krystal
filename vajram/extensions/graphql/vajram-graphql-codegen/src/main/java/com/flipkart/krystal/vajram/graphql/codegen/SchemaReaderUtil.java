package com.flipkart.krystal.vajram.graphql.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.language.TypeName;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class SchemaReaderUtil {

  public static final String DATA_FETCHER = "dataFetcher";
  public static final String REFERENCE_FETCHER = "refFetcher";
  public static final String VAJRAM_ID_ARG = "vajramId";
  public static final String SUB_PACKAGE_ARG = "subPackage";

  public static final String ENTITY_FETCHER_ARG = "entityFetcher";
  public static final String TYPE_AGGREGATOR_PACKAGE_NAME =
      "com.flipkart.fkMobileApi.dal.typeAggregators.";
  public static final String PACKAGE_NAME_ENTITY_BUILDER =
      "com.flipkart.fkMobileApi.fkEntityBuilder";
  public static final String GRAPHQL_AGGREGATOR = "GraphQLAggregator";

  static final Map<EntityTypeName, Map<GraphQlFieldSpec, ClassName>> entityTypeToReferenceFetcher =
      new HashMap<>();
  public static Map<EntityTypeName, Map<GraphQlFieldSpec, ClassName>> entityTypeToFieldResolverMap =
      new HashMap<>();
  public static Map<EntityTypeName, Map<String, String>> entityTypeRefFieldToTypeMap =
      new HashMap<>();
  private static Set<EntityTypeName> entityTypes;
  private static final Map<EntityTypeName, ClassName> entityTypeToTypeAggregator = new HashMap<>();
  private static Map<EntityTypeName, Map<ClassName, List<GraphQlFieldSpec>>>
      reverseEntityTypeToReferenceFetcher = new HashMap<>();
  static Map<EntityTypeName, Map<ClassName, List<GraphQlFieldSpec>>>
      reverseEntityTypeToFieldResolverMap = new HashMap<>();

  public static Map<String, Constructor<?>> refFieldToEntity = new HashMap<>();

  public static void setFieldVajramsForEachEntity(TypeDefinitionRegistry typeRegistry) {

    Map<EntityTypeName, ObjectTypeDefinition> entityTypesToDefinition =
        getEntityTypes(typeRegistry);

    entityTypes = entityTypesToDefinition.keySet();

    for (Map.Entry<EntityTypeName, ObjectTypeDefinition> entry :
        entityTypesToDefinition.entrySet()) {
      ObjectTypeDefinition fieldDefinition = entry.getValue();
      /* This is storing field to resolvers map */
      Map<GraphQlFieldSpec, ClassName> fieldToResolverMap = new HashMap<>();
      /* This is storing field to reference type aggregator map */
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator = new HashMap<>();
      /* This is storing resolver to field map */
      Map<ClassName, List<GraphQlFieldSpec>> reverseFieldToResolverMap = new HashMap<>();
      /* This is storing reference type agg to reference map */
      Map<ClassName, List<GraphQlFieldSpec>> reverseFieldToTypeAggregator = new HashMap<>();

      Map<String, String> refToTypeMap = new HashMap<>();
      for (FieldDefinition nestedField : fieldDefinition.getFieldDefinitions()) {
        String path = "";
        if (nestedField.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              GraphQlFieldSpec.fromField(nestedField), getDataFetcherArgs(nestedField));
        } else if (nestedField.hasDirective(REFERENCE_FETCHER)) {
          try {
            Class<?> clazz =
                Class.forName(
                    "com.flipkart.fkentity." + ((TypeName) nestedField.getType()).getName());
            refFieldToEntity.put(nestedField.getName(), clazz.getDeclaredConstructor());
          } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new RuntimeException(e);
          }
          fieldToResolverMap.put(
              GraphQlFieldSpec.fromField(nestedField), getRefFetcherArgs(nestedField));
          ObjectTypeDefinition objectTypeDefinition =
              (ObjectTypeDefinition) typeRegistry.getType(nestedField.getType()).orElseThrow();
          if (objectTypeDefinition.hasDirective("entity")) {
            String packageName =
                TYPE_AGGREGATOR_PACKAGE_NAME + objectTypeDefinition.getName().toLowerCase();
            String typeAggregator =
                getDirectiveArgumentString(objectTypeDefinition, "entity", "name").orElseThrow()
                    + GRAPHQL_AGGREGATOR;
            fieldToTypeAggregator.put(
                GraphQlFieldSpec.fromField(nestedField),
                ClassName.get(packageName, typeAggregator));
            refToTypeMap.put(
                nestedField.getName(),
                typeRegistry.getType(nestedField.getType()).orElseThrow().getName());
          }

        } else {
          dfsSchema(nestedField, path, fieldToResolverMap, typeRegistry);
        }
      }

      entityTypeToReferenceFetcher.put(entry.getKey(), fieldToTypeAggregator);
      entityTypeToFieldResolverMap.put(entry.getKey(), fieldToResolverMap);
      fieldToResolverMap.forEach(
          (key, value) ->
              reverseFieldToResolverMap.computeIfAbsent(value, k -> new ArrayList<>()).add(key));
      fieldToTypeAggregator.forEach(
          (key, value) ->
              reverseFieldToTypeAggregator.computeIfAbsent(value, k -> new ArrayList<>()).add(key));
      reverseEntityTypeToFieldResolverMap.put(entry.getKey(), reverseFieldToResolverMap);
      reverseEntityTypeToReferenceFetcher.put(entry.getKey(), reverseFieldToTypeAggregator);
      entityTypeRefFieldToTypeMap.put(entry.getKey(), refToTypeMap);
    }
  }

  private static void dfsSchema(
      FieldDefinition nestedField,
      String path,
      Map<GraphQlFieldSpec, ClassName> fieldToResolverMap,
      TypeDefinitionRegistry typeRegistry) {
    Type<?> type = nestedField.getType();
    TypeDefinition<?> typeDefinition = typeRegistry.getType(type).orElseThrow();
    String newPath = path + nestedField.getName() + ".";
    /* Check if its having dataFetcher directive */
    if (typeDefinition instanceof ObjectTypeDefinition typeDefinitionCast
        && typeDefinition.hasDirective(DATA_FETCHER)) {
      ClassName baseResolver = getDataFetcherArgs(typeDefinition);
      /* Iterate through the children fields and recursively call if they are having dataFetcher */
      for (FieldDefinition fieldDefinition : typeDefinitionCast.getFieldDefinitions()) {
        if (fieldDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              GraphQlFieldSpec.builder()
                  .fieldName(newPath + fieldDefinition.getName())
                  .fieldType(getFieldType(fieldDefinition))
                  .build(),
              getDataFetcherArgs(fieldDefinition));
        } else {
          Type typeNestedField = fieldDefinition.getType();

          if (typeNestedField instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition typeDefinitionNestedField =
                (ObjectTypeDefinition) typeRegistry.getType(typeNestedField).orElseThrow();
            if (typeDefinitionNestedField.hasDirective(DATA_FETCHER)) {
              dfsSchema(fieldDefinition, newPath, fieldToResolverMap, typeRegistry);
            } else {
              fieldToResolverMap.put(
                  GraphQlFieldSpec.builder()
                      .fieldName(newPath + fieldDefinition.getName())
                      .fieldType(getFieldType(fieldDefinition))
                      .build(),
                  baseResolver);
            }
          } else {
            fieldToResolverMap.put(
                GraphQlFieldSpec.builder()
                    .fieldName(newPath + fieldDefinition.getName())
                    .fieldType(getFieldType(fieldDefinition))
                    .build(),
                baseResolver);
          }
        }
      }
    } else {
      if (typeDefinition instanceof ScalarTypeDefinition scalarTypeDefinition) {
        if (scalarTypeDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              GraphQlFieldSpec.builder()
                  .fieldName(newPath + nestedField.getName())
                  .fieldType(getFieldType(nestedField))
                  .build(),
              getDataFetcherArgs(nestedField));
        }
      } else if (typeDefinition instanceof EnumTypeDefinition enumTypeDefinition) {
        if (enumTypeDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              GraphQlFieldSpec.builder()
                  .fieldName(newPath + nestedField.getName())
                  .fieldType(getFieldType(nestedField))
                  .build(),
              getDataFetcherArgs(nestedField));
        }
      }
    }
  }

  public static ClassName getDataFetcherArgs(DirectivesContainer<?> directivesContainer) {
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, DATA_FETCHER, SUB_PACKAGE_ARG)
            .map(s -> "." + s)
            .orElse("");
    return ClassName.get(
        PACKAGE_NAME_ENTITY_BUILDER + subPackagePart,
        getDirectiveArgumentString(directivesContainer, DATA_FETCHER, VAJRAM_ID_ARG).orElseThrow());
  }

  public static ClassName getRefFetcherArgs(DirectivesContainer<?> directivesContainer) {
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, REFERENCE_FETCHER, SUB_PACKAGE_ARG)
            .map(s -> "." + s)
            .orElse("");
    return ClassName.get(
        PACKAGE_NAME_ENTITY_BUILDER + subPackagePart,
        getDirectiveArgumentString(directivesContainer, REFERENCE_FETCHER, ENTITY_FETCHER_ARG)
            .orElseThrow());
  }

  static Map<EntityTypeName, ObjectTypeDefinition> getEntityTypes(
      TypeDefinitionRegistry typeRegistry) {
    Map<EntityTypeName, ObjectTypeDefinition> entityTypesToDefinition = new HashMap<>();

    for (TypeDefinition<?> typeDefinition : typeRegistry.types().values()) {
      List<Directive> directives = typeDefinition.getDirectives("entity");
      if (!directives.isEmpty()) {
        EntityTypeName entityType = EntityTypeName.of(typeDefinition.getName());
        entityTypesToDefinition.put(entityType, (ObjectTypeDefinition) typeDefinition);

        String packageName = TYPE_AGGREGATOR_PACKAGE_NAME + typeDefinition.getName().toLowerCase();
        String typeAggregator =
            getDirectiveArgumentString(typeDefinition, "entity", "name") + GRAPHQL_AGGREGATOR;

        entityTypeToTypeAggregator.put(entityType, ClassName.get(packageName, typeAggregator));
      }
    }
    return entityTypesToDefinition;
  }

  static com.squareup.javapoet.TypeName getFieldType(FieldDefinition field) {
    Type<?> type = field.getType();
    String typeName;
    boolean isListType = false;
    ClassName fieldTypeClassName;
    String packageName = null;
    if (type instanceof ListType listType) {
      isListType = true;

      typeName = ((TypeName) (listType).getType()).getName();
    } else {
      typeName = ((TypeName) type).getName();
    }
    for (Directive directive : field.getDirectives()) {
      if (directive.getName().equals("packageRef")) {
        for (Argument argument : directive.getArguments()) {
          if (argument.getName().equals("name")
              && argument.getValue() instanceof StringValue stringValue) {
            packageName = stringValue.getValue();
          }
          if (argument.getName().equals("class")
              && argument.getValue() instanceof StringValue stringValue) {
            typeName = stringValue.getValue();
          }
        }
      }
    }
    fieldTypeClassName =
        switch (typeName) {
          case "String" -> ClassName.get(String.class);
          case "Int" -> ClassName.get(Integer.class);
          case "Boolean" -> ClassName.get(Boolean.class);
          case "Float" -> ClassName.get(Float.class);
          case "ID" -> ClassName.get(Object.class);
          default ->
              ClassName.get(
                  Objects.requireNonNullElse(
                      packageName, TypeAggregatorAutoGenerator.PACKAGE_NAME_ENTITY),
                  typeName);
        };
    if (isListType) {
      return ParameterizedTypeName.get(
          ClassName.get(List.class), WildcardTypeName.subtypeOf(fieldTypeClassName));
    } else {
      return fieldTypeClassName;
    }
  }

  static Optional<String> getDirectiveArgumentString(
      DirectivesContainer<?> element, String directiveName, String argName) {
    Argument argument = element.getDirectives(directiveName).get(0).getArgument(argName);
    return argument == null
        ? Optional.empty()
        : Optional.ofNullable((StringValue) argument.getValue()).map(StringValue::getValue);
  }
}
