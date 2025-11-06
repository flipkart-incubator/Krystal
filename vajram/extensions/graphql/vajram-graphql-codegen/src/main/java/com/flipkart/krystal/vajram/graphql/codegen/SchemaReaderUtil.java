package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.SINGLE_FIELD_DATA_FETCHER;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.vajram.graphql.codegen.Constants.DirectiveArgs;
import com.flipkart.krystal.vajram.graphql.codegen.Constants.Directives;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.squareup.javapoet.ClassName;
import graphql.language.Argument;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.ListType;
import graphql.language.NonNullType;
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

  public static final String GRAPHQL_AGGREGATOR = "_GQlAggr";
  public static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";
  public static final String CUSTOM_TYPE_DIRECTIVE = "customType";
  public static final String PACKAGE_NAME_DIR_ARG = "packageName";
  public static final String CLASS_NAME_DIR_ARG = "className";

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<GraphQlFieldSpec, ClassName>>
      entityTypeToFieldToTypeAggregator = new HashMap<>();

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<GraphQlFieldSpec, Fetcher>> entityTypeToFieldToFetcher =
      new HashMap<>();

  @Getter(PACKAGE)
  private final Map<GraphQLTypeName, Map<Fetcher, List<GraphQlFieldSpec>>> typeToFetcherToFields =
      new HashMap<>();

  @Getter private final String rootPackageName;
  @Getter private final TypeDefinitionRegistry typeDefinitionRegistry;
  @Getter private final ImmutableMap<GraphQLTypeName, @NonNull ObjectTypeDefinition> graphQLTypes;
  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> entityTypes;
  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> composedTypes;
  @Getter private final Map<GraphQLTypeName, ObjectTypeDefinition> aggregatableTypes;

  public SchemaReaderUtil(File schemaFile) {
    this.typeDefinitionRegistry = getTypeDefinitionRegistry(schemaFile);
    this.rootPackageName = getRootPackageName(typeDefinitionRegistry);
    this.graphQLTypes = computeGraphQLTypes(typeDefinitionRegistry);
    this.entityTypes =
        Maps.filterValues(graphQLTypes, typeDef -> typeDef.hasDirective(Directives.ENTITY));
    this.composedTypes =
        Maps.filterValues(graphQLTypes, typeDef -> typeDef.hasDirective(Directives.COMPOSED_TYPE));

    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        new HashMap<>(entityTypes);
    aggregatableTypes.putAll(composedTypes);
    this.aggregatableTypes = aggregatableTypes;

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

  ClassName typeClassName(GraphQLTypeName graphQLTypeName) {
    return ClassName.get(getPackageNameForType(graphQLTypeName), graphQLTypeName.value());
  }

  ClassName entityIdClassName(ClassName entityClassName) {
    ClassName entityIdClassName =
        ClassName.get(entityClassName.packageName(), entityClassName.simpleName() + "Id");
    return entityIdClassName;
  }

  public GraphQlFieldSpec fieldSpecFromField(
      FieldDefinition fieldDefinition, String nestingPrefix, GraphQLTypeName enclosingType) {
    return GraphQlFieldSpec.builder()
        .fieldName(nestingPrefix + fieldDefinition.getName())
        .fieldDefinition(fieldDefinition)
        .enclosingType(enclosingType)
        .build();
  }

  private void setFieldVajramsForEachEntity(TypeDefinitionRegistry typeRegistry) {

    for (Entry<GraphQLTypeName, ObjectTypeDefinition> entry : aggregatableTypes.entrySet()) {
      GraphQLTypeName type = entry.getKey();

      ObjectTypeDefinition objectTypeDefinition = entry.getValue();
      /* This is storing field to resolvers map */
      Map<GraphQlFieldSpec, Fetcher> fieldToFetcherMap = new HashMap<>();
      /* This is storing field to reference type aggregator map */
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator = new HashMap<>();

      for (FieldDefinition fieldDefinition : objectTypeDefinition.getFieldDefinitions()) {
        Type<?> fieldDefinitionType = fieldDefinition.getType();
        TypeDefinition fieldTypeDefinition =
            typeRegistry.getType(fieldDefinitionType).orElseThrow();

        String path = "";
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", type),
              new Fetcher(getDataFetcherClassName(fieldDefinition), SINGLE_FIELD_DATA_FETCHER));
        } else if (fieldDefinition.hasDirective(Directives.ID_FETCHER)
            || fieldTypeDefinition.hasDirective(Directives.COMPOSED_TYPE)) {
          if (fieldDefinition.hasDirective(Directives.ID_FETCHER)) {
            fieldToFetcherMap.put(
                fieldSpecFromField(fieldDefinition, "", type),
                new Fetcher(getIdFetcherClassName(fieldDefinition), GraphQlFetcherType.ID_FETCHER));
          }
          // Unwrap ListType and NonNullType recursively to get the actual entity type
          while (fieldDefinitionType instanceof NonNullType nonNullType) {
            fieldDefinitionType = nonNullType.getType();
          }
          while (fieldDefinitionType instanceof ListType listType) {
            fieldDefinitionType = listType.getType();
            while (fieldDefinitionType instanceof NonNullType nonNullType) {
              fieldDefinitionType = nonNullType.getType();
            }
          }
          if (!(fieldDefinitionType instanceof TypeName)) {
            continue;
          }
          try {
            GraphQLTypeName graphQlTypeName = new GraphQLTypeName(fieldTypeDefinition.getName());
            String packageName = getPackageNameForType(graphQlTypeName);
            String typeAggregatorSimpleName = graphQlTypeName.value() + GRAPHQL_AGGREGATOR;

            fieldToTypeAggregator.put(
                fieldSpecFromField(fieldDefinition, "", type),
                ClassName.get(packageName, typeAggregatorSimpleName));
          } catch (Exception e) {
            // Silently ignore - type might not be an entity
          }
        } else {
          dfsSchema(fieldDefinition, path, fieldToFetcherMap, typeRegistry, type);
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

      entityTypeToFieldToFetcher.put(type, fieldToFetcherMap);
      typeToFetcherToFields.put(type, fetcherToFieldsMap);
      entityTypeToFieldToTypeAggregator.put(type, fieldToTypeAggregator);
    }
  }

  private void dfsSchema(
      FieldDefinition incomingField,
      String path,
      Map<GraphQlFieldSpec, Fetcher> fieldToResolverMap,
      TypeDefinitionRegistry typeRegistry,
      GraphQLTypeName enclosingType) {
    TypeDefinition<?> typeDefinition = typeRegistry.getType(incomingField.getType()).orElseThrow();
    String newPath = path + incomingField.getName() + ".";
    /* Check if its having dataFetcher directive */
    if (typeDefinition instanceof ObjectTypeDefinition typeDefinitionCast
        && typeDefinition.hasDirective(Directives.DATA_FETCHER)) {
      Fetcher baseFetcher =
          new Fetcher(getDataFetcherClassName(typeDefinition), MULTI_FIELD_DATA_FETCHER);
      /* Iterate through the children fields and recursively call if they are having dataFetcher */
      for (FieldDefinition fieldDefinition : typeDefinitionCast.getFieldDefinitions()) {
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(fieldDefinition, newPath, enclosingType),
              new Fetcher(getDataFetcherClassName(fieldDefinition), SINGLE_FIELD_DATA_FETCHER));
        } else if (typeRegistry.getType(fieldDefinition.getType()).orElse(null)
            instanceof ObjectTypeDefinition innerFieldTypeDef) {
          if (innerFieldTypeDef.hasDirective(Directives.DATA_FETCHER)) {
            dfsSchema(fieldDefinition, newPath, fieldToResolverMap, typeRegistry, enclosingType);
          } else {
            fieldToResolverMap.put(
                fieldSpecFromField(fieldDefinition, newPath, GraphQLTypeName.of(innerFieldTypeDef)),
                baseFetcher);
          }
        } else {
          fieldToResolverMap.put(
              fieldSpecFromField(fieldDefinition, newPath, enclosingType), baseFetcher);
        }
      }
    } else if (typeDefinition instanceof ScalarTypeDefinition
        || typeDefinition instanceof EnumTypeDefinition) {
      if (typeDefinition.hasDirective(Directives.DATA_FETCHER)) {
        fieldToResolverMap.put(
            fieldSpecFromField(incomingField, newPath, enclosingType),
            new Fetcher(getDataFetcherClassName(incomingField), SINGLE_FIELD_DATA_FETCHER));
      }
    }
  }

  private @NonNull String getPackageNameFromDirective(
      DirectivesContainer<?> directivesContainer, @Nullable String directiveName) {
    if (directiveName == null) {
      return rootPackageName;
    }
    String subPackagePart =
        getDirectiveArgumentString(directivesContainer, directiveName, DirectiveArgs.SUB_PACKAGE)
            .map(s -> "." + s)
            .orElse("");
    return rootPackageName + subPackagePart;
  }

  public ClassName getDataFetcherClassName(DirectivesContainer<?> directivesContainer) {
    String packageName = getPackageNameFromDirective(directivesContainer, Directives.DATA_FETCHER);
    return ClassName.get(
        packageName,
        getDirectiveArgumentString(
                directivesContainer, Directives.DATA_FETCHER, DirectiveArgs.VAJRAM_ID)
            .orElseThrow());
  }

  public ClassName getIdFetcherClassName(DirectivesContainer<?> directivesContainer) {
    return ClassName.get(
        getPackageNameFromDirective(directivesContainer, Directives.ID_FETCHER),
        getDirectiveArgumentString(
                directivesContainer, Directives.ID_FETCHER, DirectiveArgs.VAJRAM_ID)
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
