package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.GraphqlFetcherType.ID_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphqlFetcherType.MULTI_FIELD_DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphqlFetcherType.SINGLE_FIELD_DATA_FETCHER;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.vajram.graphql.codegen.GraphQlFieldSpec.FieldType;
import com.flipkart.krystal.vajram.graphql.codegen.GraphQlFieldSpec.ListFieldType;
import com.flipkart.krystal.vajram.graphql.codegen.GraphQlFieldSpec.SingleFieldType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
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
import graphql.language.TypeName;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class SchemaReaderUtil {

  public static final String DATA_FETCHER = "dataFetcher";
  public static final String REFERENCE_FETCHER = "idFetcher";
  public static final String VAJRAM_ID_ARG = "vajramId";
  public static final String SUB_PACKAGE_ARG = "subPackage";

  public static final String GRAPHQL_AGGREGATOR = "GraphQLAggregator";
  public static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<GraphQlFieldSpec, ClassName>>
      entityTypeToFieldToTypeAggregator = new HashMap<>();

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<GraphQlFieldSpec, Fetcher>> entityTypeToFieldToFetcher =
      new HashMap<>();

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<Fetcher, List<GraphQlFieldSpec>>>
      entityTypeToFetcherToFields = new HashMap<>();

  @Getter private final String rootPackageName;
  @Getter private final TypeDefinitionRegistry typeDefinitionRegistry;
  @Getter private final ImmutableMap<GraphQLTypeName, @NonNull ObjectTypeDefinition> graphQLTypes;
  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> entityTypes;

  public SchemaReaderUtil(File schemaFile) {
    this.typeDefinitionRegistry = getTypeDefinitionRegistry(schemaFile);
    this.rootPackageName = getRootPackageName(typeDefinitionRegistry);
    this.graphQLTypes = computeGraphQLTypes(typeDefinitionRegistry);
    this.entityTypes = Maps.filterValues(graphQLTypes, SchemaReaderUtil::isEntity);
    setFieldVajramsForEachEntity(typeDefinitionRegistry);
  }

  private static TypeDefinitionRegistry getTypeDefinitionRegistry(File schemaFile) {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    List<File> files = new ArrayList<>();

    typeDefinitionRegistry.merge(schemaParser.parse(schemaFile));

    String rootPackageName = getRootPackageName(typeDefinitionRegistry);
    String typesPath = rootPackageName.replace('.', File.separatorChar);

    File graphqlsDir = schemaFile.getParentFile().toPath().resolve(typesPath).toFile();

    String[] graphqlSchemaFileNames =
        graphqlsDir.list((dir, name) -> name.endsWith(GRAPHQL_SCHEMA_EXTENSION));
    if (graphqlSchemaFileNames == null) {
      throw new IllegalStateException("No files found in 'graphql_schemas' directory");
    } else {
      for (String graphqlSchemaFileName : graphqlSchemaFileNames) {
        File graphqlSchemaFile = new File(graphqlsDir, graphqlSchemaFileName);
        log.info("Found graphql schema file {} ", graphqlSchemaFile);
        if (!graphqlSchemaFile.exists()) {
          break;
        }
        files.add(graphqlSchemaFile);
      }
    }
    files.forEach(file -> typeDefinitionRegistry.merge(schemaParser.parse(file)));
    return typeDefinitionRegistry;
  }

  private static String getRootPackageName(TypeDefinitionRegistry typeDefinitionRegistry) {
    SchemaDefinition schemaExtensionDefinition =
        typeDefinitionRegistry.schemaDefinition().orElseThrow();
    List<Directive> rootPackages = schemaExtensionDefinition.getDirectives("rootPackage");
    if (rootPackages.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly 1 @rootPackage directive on schema definition. Found :"
              + rootPackages.size());
    }
    Directive rootPackage = rootPackages.get(0);
    return ((StringValue) rootPackage.getArgument("name").getValue()).getValue();
  }

  ClassName entityClassName(GraphQLTypeName graphQLTypeName) {
    return ClassName.get(getPackageNameForType(graphQLTypeName), graphQLTypeName.value());
  }

  ClassName entityIdClassName(ClassName entityClassName) {
    ClassName entityIdClassName =
        ClassName.get(entityClassName.packageName(), entityClassName.simpleName() + "Id");
    return entityIdClassName;
  }

  public GraphQlFieldSpec fieldSpecFromField(
      FieldDefinition fieldDefinition, String nestingPrefix) {
    return GraphQlFieldSpec.builder()
        .fieldName(nestingPrefix + fieldDefinition.getName())
        .fieldType(getFieldType(fieldDefinition, null))
        .fieldDefinition(fieldDefinition)
        .build();
  }

  private void setFieldVajramsForEachEntity(TypeDefinitionRegistry typeRegistry) {

    for (Entry<GraphQLTypeName, ObjectTypeDefinition> entry : entityTypes.entrySet()) {
      GraphQLTypeName entityType = entry.getKey();

      ObjectTypeDefinition fieldDefinition = entry.getValue();
      /* This is storing field to resolvers map */
      Map<GraphQlFieldSpec, Fetcher> fieldToFetcherMap = new HashMap<>();
      /* This is storing field to reference type aggregator map */
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator = new HashMap<>();

      for (FieldDefinition nestedField : fieldDefinition.getFieldDefinitions()) {
        String path = "";
        if (nestedField.hasDirective(DATA_FETCHER)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(nestedField, ""),
              new Fetcher(getDataFetcherClassName(nestedField), SINGLE_FIELD_DATA_FETCHER));
        } else if (nestedField.hasDirective(REFERENCE_FETCHER)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(nestedField, ""),
              new Fetcher(getIdFetcherClassName(nestedField), ID_FETCHER));
          // Unwrap ListType and NonNullType recursively to get the actual entity type
          Type<?> baseType = nestedField.getType();
          while (baseType instanceof graphql.language.NonNullType nonNullType) {
            baseType = nonNullType.getType();
          }
          while (baseType instanceof ListType listType) {
            baseType = listType.getType();
            while (baseType instanceof graphql.language.NonNullType nonNullType) {
              baseType = nonNullType.getType();
            }
          }
          if (!(baseType instanceof TypeName typeName)) {
            continue;
          }

          try {
            ObjectTypeDefinition objectTypeDefinition =
                (ObjectTypeDefinition) typeRegistry.getType(typeName).orElseThrow();
            if (objectTypeDefinition.hasDirective("entity")) {
              GraphQLTypeName graphQlTypeName = new GraphQLTypeName(objectTypeDefinition.getName());
              String packageName = getPackageNameForType(graphQlTypeName);
              String typeAggregatorSimpleName =
                  getDirectiveArgumentString(objectTypeDefinition, "entity", "name")
                          .orElse(graphQlTypeName.value())
                      + GRAPHQL_AGGREGATOR;
              ClassName aggregatorClass = ClassName.get(packageName, typeAggregatorSimpleName);
              fieldToTypeAggregator.put(fieldSpecFromField(nestedField, ""), aggregatorClass);
            }
          } catch (Exception e) {
            // Silently ignore - type might not be an entity
          }

        } else {
          dfsSchema(nestedField, path, fieldToFetcherMap, typeRegistry);
        }
      }

      Map<Fetcher, List<GraphQlFieldSpec>> fetcherToFieldsMap = new HashMap<>();
      fieldToFetcherMap.entrySet().stream()
          // Convert Map<Field, Fetcher> to Map<Fetcher, List<Field>>
          .collect(groupingBy(Entry::getValue, mapping(Entry::getKey, toList())))
          .forEach(
              (fetcher, graphQlFieldSpecs) -> {
                if (graphQlFieldSpecs.size() == 1) {
                  fetcherToFieldsMap.put(fetcher, graphQlFieldSpecs);
                } else {
                  Fetcher newFetcher = new Fetcher(fetcher.className(), MULTI_FIELD_DATA_FETCHER);
                  for (GraphQlFieldSpec graphQlFieldSpec : graphQlFieldSpecs) {
                    fieldToFetcherMap.replace(graphQlFieldSpec, newFetcher);
                  }
                  fetcherToFieldsMap.put(newFetcher, graphQlFieldSpecs);
                }
              });

      entityTypeToFieldToFetcher.put(entityType, fieldToFetcherMap);
      entityTypeToFetcherToFields.put(entityType, fetcherToFieldsMap);
      entityTypeToFieldToTypeAggregator.put(entityType, fieldToTypeAggregator);
    }
  }

  private void dfsSchema(
      FieldDefinition nestedField,
      String path,
      Map<GraphQlFieldSpec, Fetcher> fieldToResolverMap,
      TypeDefinitionRegistry typeRegistry) {
    Type<?> type = nestedField.getType();
    TypeDefinition<?> typeDefinition = typeRegistry.getType(type).orElseThrow();
    String newPath = path + nestedField.getName() + ".";
    /* Check if its having dataFetcher directive */
    if (typeDefinition instanceof ObjectTypeDefinition typeDefinitionCast
        && typeDefinition.hasDirective(DATA_FETCHER)) {
      Fetcher baseFetcher =
          new Fetcher(getDataFetcherClassName(typeDefinition), MULTI_FIELD_DATA_FETCHER);
      /* Iterate through the children fields and recursively call if they are having dataFetcher */
      for (FieldDefinition fieldDefinition : typeDefinitionCast.getFieldDefinitions()) {
        if (fieldDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(fieldDefinition, newPath),
              new Fetcher(getDataFetcherClassName(fieldDefinition), SINGLE_FIELD_DATA_FETCHER));
        } else {
          Type typeNestedField = fieldDefinition.getType();

          if (typeNestedField instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition typeDefinitionNestedField =
                (ObjectTypeDefinition) typeRegistry.getType(typeNestedField).orElseThrow();
            if (typeDefinitionNestedField.hasDirective(DATA_FETCHER)) {
              dfsSchema(fieldDefinition, newPath, fieldToResolverMap, typeRegistry);
            } else {
              fieldToResolverMap.put(fieldSpecFromField(fieldDefinition, newPath), baseFetcher);
            }
          } else {
            fieldToResolverMap.put(fieldSpecFromField(fieldDefinition, newPath), baseFetcher);
          }
        }
      }
    } else {
      if (typeDefinition instanceof ScalarTypeDefinition
          || typeDefinition instanceof EnumTypeDefinition) {
        if (typeDefinition.hasDirective(DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(nestedField, newPath),
              new Fetcher(getDataFetcherClassName(nestedField), SINGLE_FIELD_DATA_FETCHER));
        }
      }
    }
  }

  private @NonNull String getPackageNameFromDirective(
      DirectivesContainer<?> directivesContainer, @Nullable String directiveName) {
    if (directiveName == null) {
      return rootPackageName;
    }
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, directiveName, SUB_PACKAGE_ARG)
            .map(s -> "." + s)
            .orElse("");
    return rootPackageName + subPackagePart;
  }

  public ClassName getDataFetcherClassName(DirectivesContainer<?> directivesContainer) {
    String packageName = getPackageNameFromDirective(directivesContainer, DATA_FETCHER);
    return ClassName.get(
        packageName,
        getDirectiveArgumentString(directivesContainer, DATA_FETCHER, VAJRAM_ID_ARG).orElseThrow());
  }

  public ClassName getIdFetcherClassName(DirectivesContainer<?> directivesContainer) {
    return ClassName.get(
        getPackageNameFromDirective(directivesContainer, REFERENCE_FETCHER),
        getDirectiveArgumentString(directivesContainer, REFERENCE_FETCHER, VAJRAM_ID_ARG)
            .orElseThrow());
  }

  private static ImmutableMap<GraphQLTypeName, ObjectTypeDefinition> computeGraphQLTypes(
      TypeDefinitionRegistry typeRegistry) {
    Map<GraphQLTypeName, ObjectTypeDefinition> entityTypesToDefinition = new HashMap<>();
    for (TypeDefinition<?> typeDefinition : typeRegistry.types().values()) {
      if (typeDefinition instanceof ObjectTypeDefinition objectTypeDefinition) {
        entityTypesToDefinition.put(
            GraphQLTypeName.of(typeDefinition.getName()), objectTypeDefinition);
      }
    }
    return ImmutableMap.copyOf(entityTypesToDefinition);
  }

  static boolean isEntity(TypeDefinition<?> typeDefinition) {
    return !typeDefinition.getDirectives("entity").isEmpty();
  }

  FieldType getFieldType(FieldDefinition field, @Nullable GraphQLTypeName graphqlTypeName) {
    Type<?> type = field.getType();
    GraphQLTypeName typeName;
    boolean isListType = false;
    ClassName fieldTypeClassName;
    String packageName = null;
    
    // Unwrap NonNullType and ListType recursively to get to TypeName
    Type<?> baseType = type;
    while (baseType instanceof graphql.language.NonNullType nonNullType) {
      baseType = nonNullType.getType();
    }
    if (baseType instanceof ListType listType) {
      isListType = true;
      baseType = listType.getType();
      while (baseType instanceof graphql.language.NonNullType nonNullType) {
        baseType = nonNullType.getType();
      }
    }
    if (!(baseType instanceof TypeName typeNameNode)) {
      throw new IllegalArgumentException("Unable to extract TypeName from field: " + field.getName());
    }
    typeName = new GraphQLTypeName(typeNameNode.getName());
    for (Directive directive : field.getDirectives()) {
      if (directive.getName().equals("packageRef")) {
        for (Argument argument : directive.getArguments()) {
          if (argument.getName().equals("name")
              && argument.getValue() instanceof StringValue stringValue) {
            packageName = stringValue.getValue();
          }
          if (argument.getName().equals("class")
              && argument.getValue() instanceof StringValue stringValue) {
            typeName = new GraphQLTypeName(stringValue.getValue());
          }
        }
      }
    }
    fieldTypeClassName =
        switch (typeName.value()) {
          case "String" -> ClassName.get(String.class);
          case "Int" -> ClassName.get(Integer.class);
          case "Boolean" -> ClassName.get(Boolean.class);
          case "Float" -> ClassName.get(Float.class);
          case "ID" -> graphqlTypeName != null
              ? entityIdClassName(
                  ClassName.get(getPackageNameForType(graphqlTypeName), graphqlTypeName.value()))
              : ClassName.get(Object.class);
          default -> ClassName.get(
              requireNonNullElse(packageName, getPackageNameForType(typeName)), typeName.value());
        };

    if (isListType) {
      return new ListFieldType(
          ParameterizedTypeName.get(ClassName.get(List.class), fieldTypeClassName),
          fieldTypeClassName);
    } else {
      return new SingleFieldType(fieldTypeClassName);
    }
  }

  static Optional<String> getDirectiveArgumentString(
      DirectivesContainer<?> element, String directiveName, String argName) {
    List<Directive> directives = element.getDirectives(directiveName);
    if (directives.isEmpty()) {
      return Optional.empty();
    }
    Argument argument = directives.get(0).getArgument(argName);
    return argument == null
        ? Optional.empty()
        : Optional.ofNullable((StringValue) argument.getValue()).map(StringValue::getValue);
  }

  String getPackageNameForType(GraphQLTypeName graphQLTypeName) {
    return rootPackageName + "." + graphQLTypeName.value().toLowerCase();
  }
}
