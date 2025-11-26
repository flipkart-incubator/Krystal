package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.api.execution.QueryAnalyseUtil.DEFAULT_ENTITY_ID_FIELD;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.MULTI_FIELD_DATA_FETCHER;
import static com.flipkart.krystal.vajram.graphql.codegen.GraphQlFetcherType.SINGLE_FIELD_DATA_FETCHER;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.vajram.graphql.api.Constants;
import com.flipkart.krystal.vajram.graphql.api.Constants.DirectiveArgs;
import com.flipkart.krystal.vajram.graphql.api.Constants.Directives;
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
import graphql.language.OperationTypeDefinition;
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
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class SchemaReaderUtil {

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

  @Getter
  private final ImmutableMap<@NonNull GraphQLTypeName, @NonNull ObjectTypeDefinition>
      graphQLObjectTypes;

  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> entityTypes;
  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> composedTypes;

  /** Types which need a GraphqlAggregate vajram generated */
  @Getter private final Map<GraphQLTypeName, ObjectTypeDefinition> aggregatableTypes;

  @Getter private final Map<GraphQLTypeName, ObjectTypeDefinition> operationTypes;

  @Getter private final @Nullable ObjectTypeDefinition queryType;
  @Getter private final @Nullable ObjectTypeDefinition mutationType;
  @Getter private final @Nullable ObjectTypeDefinition subscriptionType;

  public SchemaReaderUtil(File schemaFile) {
    this.typeDefinitionRegistry = getTypeDefinitionRegistry(schemaFile);
    this.rootPackageName = getRootPackageName(typeDefinitionRegistry);
    this.graphQLObjectTypes = computeGraphQLTypes(typeDefinitionRegistry);
    this.entityTypes =
        Maps.filterValues(graphQLObjectTypes, typeDef -> typeDef.hasDirective(Directives.ENTITY));
    this.composedTypes =
        Maps.filterValues(
            graphQLObjectTypes, typeDef -> typeDef.hasDirective(Directives.COMPOSED_TYPE));

    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        new HashMap<>(entityTypes);
    aggregatableTypes.putAll(composedTypes);

    Optional<SchemaDefinition> schemaDefinition = typeDefinitionRegistry.schemaDefinition();
    if (schemaDefinition.isEmpty()) {
      throw new IllegalStateException(
          "Schema definition is mandatory. Could not find Schema definition.");
    }
    Map<String, OperationTypeDefinition> operationTypesByOpName =
        schemaDefinition.get().getOperationTypeDefinitions().stream()
            .collect(Collectors.toMap(OperationTypeDefinition::getName, op -> op));
    Map<GraphQLTypeName, OperationTypeDefinition> operationTypesByType =
        schemaDefinition.get().getOperationTypeDefinitions().stream()
            .collect(
                Collectors.toMap(
                    operationTypeDefinition ->
                        GraphQLTypeName.of(operationTypeDefinition.getTypeName().getName()),
                    op -> op));
    this.operationTypes = Maps.filterKeys(graphQLObjectTypes, operationTypesByType::containsKey);

    OperationTypeDefinition queryOpDef = operationTypesByOpName.get("query");
    if (queryOpDef != null) {
      GraphQLTypeName queryTypeName = GraphQLTypeName.of(queryOpDef.getTypeName().getName());
      this.queryType = graphQLObjectTypes.get(queryTypeName);
      if (this.queryType != null) {
        aggregatableTypes.put(queryTypeName, queryType);
      }
    } else {
      this.queryType = null;
    }

    OperationTypeDefinition mutationOpDef = operationTypesByOpName.get("mutation");
    if (mutationOpDef != null) {
      GraphQLTypeName queryTypeName = GraphQLTypeName.of(mutationOpDef.getTypeName().getName());
      this.mutationType = graphQLObjectTypes.get(queryTypeName);
      if (this.mutationType != null) {
        aggregatableTypes.put(queryTypeName, mutationType);
      }
    } else {
      this.mutationType = null;
    }

    OperationTypeDefinition subscriptionOpDef = operationTypesByOpName.get("subscription");
    if (subscriptionOpDef != null) {
      GraphQLTypeName queryTypeName = GraphQLTypeName.of(subscriptionOpDef.getTypeName().getName());
      this.subscriptionType = graphQLObjectTypes.get(queryTypeName);
      if (this.subscriptionType != null) {
        aggregatableTypes.put(queryTypeName, subscriptionType);
      }
    } else {
      this.subscriptionType = null;
    }

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
    if (graphqlSchemaFileNames != null) {
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
    List<Directive> rootPackages = schemaExtensionDefinition.getDirectives(Directives.ROOT_PACKAGE);
    if (rootPackages.size() != 1) {
      throw new IllegalStateException(
          "Expected exactly 1 @rootPackage directive on schema definition. Found :"
              + rootPackages.size());
    }
    Directive rootPackage = rootPackages.get(0);
    return ((StringValue) rootPackage.getArgument(DirectiveArgs.NAME).getValue()).getValue();
  }

  ClassName typeClassName(GraphQLTypeName graphQLTypeName) {
    return ClassName.get(getPackageNameForType(graphQLTypeName), graphQLTypeName.value());
  }

  boolean hasEntityId(TypeDefinition typeDefinition) {
    if (!(typeDefinition instanceof ObjectTypeDefinition objectTypeDefinition)) {
      return false;
    }
    return objectTypeDefinition.hasDirective(Directives.ENTITY)
        || objectTypeDefinition.hasDirective(Directives.COMPOSED_TYPE);
  }

  ClassName entityIdClassName(GraphQLTypeName graphQLTypeName) {
    Optional<TypeDefinition> typeDefinition =
        typeDefinitionRegistry.getType(graphQLTypeName.value());
    if (typeDefinition.isEmpty()
        || !(typeDefinition.get() instanceof ObjectTypeDefinition objectTypeDefinition)) {
      throw new IllegalArgumentException("Only ObjectTypeDefinitions can have entity ids");
    }

    boolean isEntity = objectTypeDefinition.hasDirective(Directives.ENTITY);

    Optional<String> composedInEntity =
        getDirectiveArgumentString(
            objectTypeDefinition, Directives.COMPOSED_TYPE, DirectiveArgs.IN_ENTITY);
    if (composedInEntity.isPresent()) {
      return entityIdClassName(typeClassName(GraphQLTypeName.of(composedInEntity.get())));
    }
    if (isEntity) {
      return entityIdClassName(typeClassName(graphQLTypeName));
    }
    throw new IllegalArgumentException("Only '@entity' and '@composedType' can have entity ids");
  }

  private ClassName entityIdClassName(ClassName entityClassName) {
    return ClassName.get(entityClassName.packageName(), entityClassName.simpleName() + "Id");
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
      GraphQLTypeName parentType = entry.getKey();

      ObjectTypeDefinition objectTypeDefinition = entry.getValue();
      /* This is storing field to resolvers map */
      Map<GraphQlFieldSpec, Fetcher> fieldToFetcherMap = new HashMap<>();
      /* This is storing field to reference type aggregator map */
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator = new HashMap<>();

      for (FieldDefinition fieldDefinition : objectTypeDefinition.getFieldDefinitions()) {
        Type<?> fieldDefinitionType = fieldDefinition.getType();
        TypeDefinition fieldTypeDefinition =
            typeRegistry
                .getType(fieldDefinitionType)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Could not find type for field: " + fieldDefinition));

        String path = "";
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new VajramFetcher(
                  getDataFetcherClassName(fieldDefinition), SINGLE_FIELD_DATA_FETCHER));
        } else if (fieldTypeDefinition.hasDirective(Directives.ENTITY)
            && fieldDefinition.hasDirective(Directives.ID_FETCHER)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new VajramFetcher(
                  getIdFetcherClassName(fieldDefinition), GraphQlFetcherType.ID_FETCHER));
          addAggregator(fieldDefinition, fieldTypeDefinition, parentType, fieldToTypeAggregator);
        } else if (fieldTypeDefinition.hasDirective(Directives.ENTITY)
            && fieldDefinition.hasDirective(Directives.INHERIT_ID_FROM_ARGS)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new SimpleFetcher(GraphQlFetcherType.INHERIT_ID_FROM_ARGS));
          addAggregator(fieldDefinition, fieldTypeDefinition, parentType, fieldToTypeAggregator);
        } else if (fieldTypeDefinition.hasDirective(Directives.COMPOSED_TYPE)
            && fieldDefinition.hasDirective(Directives.INHERIT_ID_FROM_PARENT)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new SimpleFetcher(GraphQlFetcherType.INHERIT_ID_FROM_PARENT));
          addAggregator(fieldDefinition, fieldTypeDefinition, parentType, fieldToTypeAggregator);
        } else {
          dfsSchema(fieldDefinition, path, fieldToFetcherMap, typeRegistry, parentType);
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
                } else if (fetcher instanceof VajramFetcher vajramFetcher) {
                  Fetcher newFetcher =
                      new VajramFetcher(vajramFetcher.vajramClassName(), MULTI_FIELD_DATA_FETCHER);
                  for (GraphQlFieldSpec graphQlFieldSpec : graphQlFieldSpecs) {
                    fieldToFetcherMap.replace(graphQlFieldSpec, newFetcher);
                  }
                  fetcherToFieldsMap.put(newFetcher, graphQlFieldSpecs);
                }
              });

      entityTypeToFieldToFetcher.put(parentType, fieldToFetcherMap);
      typeToFetcherToFields.put(parentType, fetcherToFieldsMap);
      entityTypeToFieldToTypeAggregator.put(parentType, fieldToTypeAggregator);
    }
  }

  private void addAggregator(
      FieldDefinition fieldDefinition,
      TypeDefinition fieldTypeDefinition,
      GraphQLTypeName type,
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator) {
    Type<?> fieldDefinitionType = fieldDefinition.getType();
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
      return;
    }
    try {
      GraphQLTypeName graphQlTypeName = new GraphQLTypeName(fieldTypeDefinition.getName());
      String packageName = getPackageNameForType(graphQlTypeName);
      String typeAggregatorSimpleName =
          graphQlTypeName.value() + Constants.GRAPHQL_AGGREGATOR_SUFFIX;

      fieldToTypeAggregator.put(
          fieldSpecFromField(fieldDefinition, "", type),
          ClassName.get(packageName, typeAggregatorSimpleName));
    } catch (Exception e) {
      // Silently ignore - type might not be an entity
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
          new VajramFetcher(getDataFetcherClassName(typeDefinition), MULTI_FIELD_DATA_FETCHER);
      /* Iterate through the children fields and recursively call if they are having dataFetcher */
      for (FieldDefinition fieldDefinition : typeDefinitionCast.getFieldDefinitions()) {
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(fieldDefinition, newPath, enclosingType),
              new VajramFetcher(
                  getDataFetcherClassName(fieldDefinition), SINGLE_FIELD_DATA_FETCHER));
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
            new VajramFetcher(getDataFetcherClassName(incomingField), SINGLE_FIELD_DATA_FETCHER));
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
    TypeDefinition objectTypeDefinition =
        requireNonNull(
            typeDefinitionRegistry().types().get(graphQLTypeName.value()),
            () -> "Could not find type definition for type: " + graphQLTypeName);
    String subPackage =
        getDirectiveArgumentString(objectTypeDefinition, Directives.SUB_PACKAGE, DirectiveArgs.NAME)
            .orElse(graphQLTypeName.value().toLowerCase())
            .trim();
    if (!subPackage.isEmpty()) {
      subPackage = "." + subPackage;
    }
    return rootPackageName + subPackage;
  }

  public String getEntityIdFieldName(TypeDefinition fieldTypeDef) {
    return getDirectiveArgumentString(
            fieldTypeDef, Directives.ENTITY, DirectiveArgs.ENTITY_ID_FIELD)
        .orElse(DEFAULT_ENTITY_ID_FIELD);
  }

  public Optional<GraphQLTypeName> getComposingEntityType(ObjectTypeDefinition typeDefinition) {
    return typeDefinition.hasDirective(Directives.ENTITY)
        ? Optional.of(GraphQLTypeName.of(typeDefinition))
        : getDirectiveArgumentString(
                typeDefinition, Directives.COMPOSED_TYPE, DirectiveArgs.IN_ENTITY)
            .map(GraphQLTypeName::of);
  }
}
