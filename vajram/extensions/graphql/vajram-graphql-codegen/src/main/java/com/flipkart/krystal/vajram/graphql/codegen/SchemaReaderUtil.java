package com.flipkart.krystal.vajram.graphql.codegen;

import static java.util.Objects.requireNonNullElse;

import com.flipkart.krystal.vajram.codegen.common.models.CodeGenUtility;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.NamedNode;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.StringValue;
import graphql.language.Type;
import graphql.language.TypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.Getter;

public class SchemaReaderUtil {

  public static final String DATA_FETCHER = "dataFetcher";
  public static final String REFERENCE_FETCHER = "refFetcher";
  public static final String VAJRAM_ID_ARG = "vajramId";
  public static final String SUB_PACKAGE_ARG = "subPackage";

  public static final String TYPE_AGGREGATOR_PACKAGE_NAME =
      "com.flipkart.fkMobileApi.dal.typeAggregators.";
  public static final String PACKAGE_NAME_ENTITY_BUILDER =
      "com.flipkart.fkMobileApi.fkEntityBuilder";
  public static final String GRAPHQL_AGGREGATOR = "GraphQLAggregator";
  public static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";

  static final Map<GraphQLTypeName, Map<GraphQlFieldSpec, ClassName>> entityTypeToReferenceFetcher =
      new HashMap<>();
  static final Map<GraphQLTypeName, Map<ClassName, List<GraphQlFieldSpec>>>
      reverseEntityTypeToFieldResolverMap = new HashMap<>();

  @Getter private final String rootPackageName;
  @Getter private final TypeDefinitionRegistry typeDefinitionRegistry;

  public SchemaReaderUtil(CodeGenUtility util) {
    this.typeDefinitionRegistry = getTypeDefinitionRegistry(util);
    this.rootPackageName = getRootPackageName(util, typeDefinitionRegistry);
    setFieldVajramsForEachEntity(typeDefinitionRegistry);
  }

  private static TypeDefinitionRegistry getTypeDefinitionRegistry(CodeGenUtility util) {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    List<File> files = new ArrayList<>();
    FileObject schemaFileObject;
    try {
      schemaFileObject =
          util.processingEnv()
              .getFiler()
              .getResource(StandardLocation.SOURCE_PATH, "", "Schema.graphqls");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      typeDefinitionRegistry.merge(
          schemaParser.parse(schemaFileObject.getCharContent(false).toString()));

      String rootPackageName = getRootPackageName(util, typeDefinitionRegistry);
      String typesPath = rootPackageName.replace('.', File.separatorChar);

      File graphqlsDir =
          new File(schemaFileObject.toUri()).getParentFile().toPath().resolve(typesPath).toFile();

      String[] graphqlSchemaFileNames =
          graphqlsDir.list((dir, name) -> name.endsWith(GRAPHQL_SCHEMA_EXTENSION));
      if (graphqlSchemaFileNames == null) {
        util.error("No files found in 'graphql_schemas' directory");
      } else {
        for (String graphqlSchemaFileName : graphqlSchemaFileNames) {
          File graphqlSchemaFile = new File(graphqlsDir, graphqlSchemaFileName);
          util.note("Found graphql schema file: " + graphqlSchemaFile);
          if (!graphqlSchemaFile.exists()) {
            break;
          }
          files.add(graphqlSchemaFile);
        }
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    files.forEach(file -> typeDefinitionRegistry.merge(schemaParser.parse(file)));
    return typeDefinitionRegistry;
  }

  private static String getRootPackageName(
      CodeGenUtility util, TypeDefinitionRegistry typeDefinitionRegistry) {
    SchemaDefinition schemaExtensionDefinition = typeDefinitionRegistry.schemaDefinition().get();
    List<Directive> rootPackages = schemaExtensionDefinition.getDirectives("rootPackage");
    if (rootPackages.size() != 1) {
      util.error(
          "Expected exactly 1 @rootPackage directive on schema definition. Found :"
              + rootPackages.size());
    }
    Directive rootPackage = rootPackages.get(0);
    String rootPackageName = ((StringValue) rootPackage.getArgument("name").getValue()).getValue();
    return rootPackageName;
  }

  public GraphQlFieldSpec fieldSpecFromField(
      FieldDefinition fieldDefinition, String nestingPrefix) {
    return GraphQlFieldSpec.builder()
        .fieldName(nestingPrefix + fieldDefinition.getName())
        .fieldType(getFieldType(fieldDefinition))
        .fieldDefinition(fieldDefinition)
        .build();
  }

  private void setFieldVajramsForEachEntity(TypeDefinitionRegistry typeRegistry) {

    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypesToDefinition =
        getEntityTypes(typeRegistry);

    for (Map.Entry<GraphQLTypeName, ObjectTypeDefinition> entry :
        entityTypesToDefinition.entrySet()) {
      ObjectTypeDefinition fieldDefinition = entry.getValue();
      /* This is storing field to resolvers map */
      Map<GraphQlFieldSpec, ClassName> fieldToResolverMap = new HashMap<>();
      /* This is storing field to reference type aggregator map */
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator = new HashMap<>();
      /* This is storing resolver to field map */
      Map<ClassName, List<GraphQlFieldSpec>> reverseFieldToResolverMap = new HashMap<>();

      for (FieldDefinition nestedField : fieldDefinition.getFieldDefinitions()) {
        String path = "";
        if (nestedField.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(nestedField, ""), getDataFetcherArgs(nestedField));
        } else if (nestedField.hasDirective(REFERENCE_FETCHER)) {
          //          try {
          //            Class<?> clazz =
          //                Class.forName(
          //                    "com.flipkart.fkentity." + ((TypeName)
          // nestedField.getType()).getName());
          //            refFieldToEntity.put(nestedField.getName(), clazz.getDeclaredConstructor());
          //          } catch (ClassNotFoundException | NoSuchMethodException e) {
          //            throw new RuntimeException(e);
          //          }
          fieldToResolverMap.put(
              fieldSpecFromField(nestedField, ""), getRefFetcherArgs(nestedField));
          ObjectTypeDefinition objectTypeDefinition =
              (ObjectTypeDefinition) typeRegistry.getType(nestedField.getType()).orElseThrow();
          if (objectTypeDefinition.hasDirective("entity")) {
            String packageName = getPackageNameForType(objectTypeDefinition.getName());
            String typeAggregator =
                getDirectiveArgumentString(objectTypeDefinition, "entity", "name").orElseThrow()
                    + GRAPHQL_AGGREGATOR;
            fieldToTypeAggregator.put(
                fieldSpecFromField(nestedField, ""), ClassName.get(packageName, typeAggregator));
          }

        } else {
          dfsSchema(nestedField, path, fieldToResolverMap, typeRegistry);
        }
      }

      entityTypeToReferenceFetcher.put(entry.getKey(), fieldToTypeAggregator);
      fieldToResolverMap.forEach(
          (key, value) ->
              reverseFieldToResolverMap.computeIfAbsent(value, k -> new ArrayList<>()).add(key));
      reverseEntityTypeToFieldResolverMap.put(entry.getKey(), reverseFieldToResolverMap);
    }
  }

  private void dfsSchema(
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
              fieldSpecFromField(fieldDefinition, newPath), getDataFetcherArgs(fieldDefinition));
        } else {
          Type typeNestedField = fieldDefinition.getType();

          if (typeNestedField instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition typeDefinitionNestedField =
                (ObjectTypeDefinition) typeRegistry.getType(typeNestedField).orElseThrow();
            if (typeDefinitionNestedField.hasDirective(DATA_FETCHER)) {
              dfsSchema(fieldDefinition, newPath, fieldToResolverMap, typeRegistry);
            } else {
              fieldToResolverMap.put(fieldSpecFromField(fieldDefinition, newPath), baseResolver);
            }
          } else {
            fieldToResolverMap.put(fieldSpecFromField(fieldDefinition, newPath), baseResolver);
          }
        }
      }
    } else {
      if (typeDefinition instanceof ScalarTypeDefinition scalarTypeDefinition) {
        if (scalarTypeDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(nestedField, newPath), getDataFetcherArgs(nestedField));
        }
      } else if (typeDefinition instanceof EnumTypeDefinition enumTypeDefinition) {
        if (enumTypeDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(nestedField, newPath), getDataFetcherArgs(nestedField));
        }
      }
    }
  }

  public ClassName getDataFetcherArgs(DirectivesContainer<?> directivesContainer) {
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, DATA_FETCHER, SUB_PACKAGE_ARG)
            .map(s -> "." + s)
            .orElse("");
    return ClassName.get(
        rootPackageName + subPackagePart,
        getDirectiveArgumentString(directivesContainer, DATA_FETCHER, VAJRAM_ID_ARG).orElseThrow());
  }

  public ClassName getRefFetcherArgs(DirectivesContainer<?> directivesContainer) {
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, REFERENCE_FETCHER, SUB_PACKAGE_ARG)
            .map(s -> "." + s)
            .orElse("");
    return ClassName.get(
        rootPackageName + subPackagePart,
        getDirectiveArgumentString(directivesContainer, REFERENCE_FETCHER, VAJRAM_ID_ARG)
            .orElseThrow());
  }

  Map<GraphQLTypeName, ObjectTypeDefinition> getEntityTypes(TypeDefinitionRegistry typeRegistry) {
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypesToDefinition = new HashMap<>();
    for (TypeDefinition<?> typeDefinition : typeRegistry.types().values()) {
      List<Directive> directives = typeDefinition.getDirectives("entity");
      if (!directives.isEmpty()) {
        GraphQLTypeName entityType = GraphQLTypeName.of(typeDefinition.getName());
        entityTypesToDefinition.put(entityType, (ObjectTypeDefinition) typeDefinition);
      }
    }
    return entityTypesToDefinition;
  }

  TypeName getFieldType(FieldDefinition field) {
    Type<?> type = field.getType();
    String typeName;
    boolean isListType = false;
    ClassName fieldTypeClassName;
    String packageName = null;
    if (type instanceof ListType listType) {
      isListType = true;
      //noinspection unchecked
      typeName = ((NamedNode<graphql.language.TypeName>) (listType).getType()).getName();
    } else {
      //noinspection unchecked
      typeName = ((NamedNode<graphql.language.TypeName>) type).getName();
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
                  requireNonNullElse(packageName, getPackageNameForType(typeName)), typeName);
        };
    if (isListType) {
      return ParameterizedTypeName.get(ClassName.get(List.class), fieldTypeClassName);
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

  public String getPackageNameForType(String typeName) {
    return rootPackageName + "." + typeName.toLowerCase();
  }
}
