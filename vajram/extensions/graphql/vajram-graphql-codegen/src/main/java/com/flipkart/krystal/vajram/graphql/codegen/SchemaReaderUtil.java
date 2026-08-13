package com.flipkart.krystal.vajram.graphql.codegen;

import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_AGGREGATOR_SUFFIX;
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
import graphql.language.BooleanValue;
import graphql.language.Directive;
import graphql.language.DirectivesContainer;
import graphql.language.EnumTypeDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputValueDefinition;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public class SchemaReaderUtil {

  public static final String GRAPHQL_SCHEMA_EXTENSION = ".graphqls";
  public static final String JAVA_TYPE_DIRECTIVE = "javaType";
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
  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> entityExtensions;

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
    this.entityExtensions =
        Maps.filterValues(
            graphQLObjectTypes, typeDef -> typeDef.hasDirective(Directives.ENTITY_EXTENSION));

    Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> aggregatableTypes =
        new HashMap<>(graphQLObjectTypes);

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
    validateDirectiveInvariants();
    setFieldVajramsForEachEntity(
        entityTypeToFieldToTypeAggregator,
        entityTypeToFieldToFetcher,
        typeToFetcherToFields,
        typeDefinitionRegistry,
        aggregatableTypes,
        rootPackageName);
  }

  private void validateDirectiveInvariants() {
    graphQLObjectTypes.forEach(
        (typeName, typeDefinition) -> {
          for (FieldDefinition field : typeDefinition.getFieldDefinitions()) {
            TypeDefinition<?> fieldType = typeDefinitionFor(field);
            if (field.hasDirective(Directives.ID_FIELD)) {
              if (!field.getInputValueDefinitions().isEmpty()) {
                throw invalid(
                    "@idField '%s.%s' cannot declare arguments", typeName.value(), field.getName());
              }
              if (!(fieldType instanceof ScalarTypeDefinition)
                  && !(fieldType instanceof EnumTypeDefinition)) {
                throw invalid(
                    "@idField '%s.%s' must have a scalar or enum type",
                    typeName.value(), field.getName());
              }
            }
            if (field.hasDirective(Directives.DATA_FETCHER)
                && !(fieldType instanceof ScalarTypeDefinition)
                && !(fieldType instanceof EnumTypeDefinition)) {
              throw invalid(
                  "@dataFetcher '%s.%s' can only be used on scalar or enum fields",
                  typeName.value(), field.getName());
            }
            if (field.hasDirective(Directives.INHERIT_ID_FROM_ARGS)) {
              validateInferIdFromArgs(typeName, field, fieldType);
            }
            if (field.hasDirective(Directives.INHERIT_ID_FROM_PARENT)) {
              validateInferIdFromParent(typeName, typeDefinition, field, fieldType);
            }
          }
        });
    validateEntityExtensionReferences();
  }

  private void validateInferIdFromArgs(
      GraphQLTypeName parentType, FieldDefinition field, TypeDefinition<?> fieldType) {
    if (!isOperationType(parentType)) {
      throw invalid(
          "@inferIdFromArgs '%s.%s' is only valid on root operation fields",
          parentType.value(), field.getName());
    }
    if (!(fieldType instanceof ObjectTypeDefinition objectType)) {
      throw invalid(
          "@inferIdFromArgs '%s.%s' must return an object type",
          parentType.value(), field.getName());
    }
    Map<String, InputValueDefinition> argsByName =
        field.getInputValueDefinitions().stream()
            .collect(Collectors.toMap(InputValueDefinition::getName, input -> input));
    for (FieldDefinition idField : nonNullIdFields(objectType)) {
      InputValueDefinition arg = argsByName.get(idField.getName());
      if (arg == null || !arg.getType().toString().equals(idField.getType().toString())) {
        throw invalid(
            "@inferIdFromArgs '%s.%s' requires an argument '%s' with type '%s' for @idField '%s.%s'",
            parentType.value(),
            field.getName(),
            idField.getName(),
            idField.getType(),
            objectType.getName(),
            idField.getName());
      }
    }
  }

  private void validateInferIdFromParent(
      GraphQLTypeName parentType,
      ObjectTypeDefinition parent,
      FieldDefinition field,
      TypeDefinition<?> fieldType) {
    if (isOperationType(parentType)) {
      throw invalid(
          "@inferIdFromParent '%s.%s' is not valid on root operation fields",
          parentType.value(), field.getName());
    }
    if (isList(field.getType()) || !field.getInputValueDefinitions().isEmpty()) {
      throw invalid(
          "@inferIdFromParent '%s.%s' cannot be used on list fields or fields with arguments",
          parentType.value(), field.getName());
    }
    if (!(fieldType instanceof ObjectTypeDefinition extension)
        || !extension.hasDirective(Directives.ENTITY_EXTENSION)) {
      throw invalid(
          "@inferIdFromParent '%s.%s' must return an @entityExtension",
          parentType.value(), field.getName());
    }
    if (!getComposingEntityType(parent).equals(getComposingEntityType(extension))) {
      throw invalid(
          "@inferIdFromParent '%s.%s' must reference an @entityExtension of '%s'",
          parentType.value(),
          field.getName(),
          getComposingEntityType(parent).map(GraphQLTypeName::value).orElse(parentType.value()));
    }
  }

  private void validateEntityExtensionReferences() {
    entityExtensions.forEach(
        (extensionName, extension) -> {
          GraphQLTypeName extendedEntity =
              entityExtensionOf(extension, extensionName)
                  .orElseThrow(
                      () ->
                          invalid(
                              "@entityExtension '%s' must declare ofEntity",
                              extensionName.value()));
          if (!entityTypes.containsKey(extendedEntity)) {
            throw invalid(
                "@entityExtension '%s' must reference an @entity with ofEntity, found '%s'",
                extensionName.value(), extendedEntity.value());
          }
        });
    graphQLObjectTypes.forEach(
        (parentName, parent) -> {
          Set<GraphQLTypeName> extensionTypes = new HashSet<>();
          for (FieldDefinition field : parent.getFieldDefinitions()) {
            ObjectTypeDefinition extension =
                typeDefinitionFor(field) instanceof ObjectTypeDefinition objectType
                        && objectType.hasDirective(Directives.ENTITY_EXTENSION)
                    ? objectType
                    : null;
            if (extension == null) {
              continue;
            }
            if (!parent.hasDirective(Directives.ENTITY)
                && !parent.hasDirective(Directives.ENTITY_EXTENSION)) {
              throw invalid(
                  "Field '%s.%s' returns an @entityExtension but its parent is neither an @entity nor an @entityExtension",
                  parentName.value(), field.getName());
            }
            if (isList(field.getType())) {
              throw invalid(
                  "Field '%s.%s' returning an @entityExtension cannot be a list",
                  parentName.value(), field.getName());
            }
            GraphQLTypeName extensionName = GraphQLTypeName.of(extension);
            if (!extensionTypes.add(extensionName)) {
              throw invalid(
                  "@entityExtension '%s' can occur at most once as a field type in '%s'",
                  extensionName.value(), parentName.value());
            }
            GraphQLTypeName expectedEntity =
                getComposingEntityType(parent).orElse(GraphQLTypeName.of(parent));
            GraphQLTypeName extensionEntity =
                entityExtensionOf(extension, GraphQLTypeName.of(extension)).orElseThrow();
            if (!expectedEntity.equals(extensionEntity)) {
              throw invalid(
                  "Field '%s.%s' must return an @entityExtension of '%s', but '%s' extends '%s'",
                  parentName.value(),
                  field.getName(),
                  expectedEntity.value(),
                  extension.getName(),
                  extensionEntity.value());
            }
          }
        });
  }

  private Optional<GraphQLTypeName> entityExtensionOf(
      ObjectTypeDefinition extension, GraphQLTypeName extensionName) {
    return getDirectiveArgumentString(
            extension, Directives.ENTITY_EXTENSION, DirectiveArgs.OF_ENTITY)
        .map(GraphQLTypeName::of);
  }

  private TypeDefinition<?> typeDefinitionFor(FieldDefinition field) {
    return typeDefinitionRegistry
        .getType(field.getType())
        .orElseThrow(() -> invalid("Could not find type for field '%s'", field));
  }

  private static List<FieldDefinition> nonNullIdFields(ObjectTypeDefinition type) {
    return type.getFieldDefinitions().stream()
        .filter(field -> field.hasDirective(Directives.ID_FIELD))
        .filter(field -> field.getType() instanceof NonNullType)
        .toList();
  }

  private static boolean isList(Type<?> type) {
    while (type instanceof NonNullType nonNullType) {
      type = nonNullType.getType();
    }
    return type instanceof ListType;
  }

  private static String declaredTypeName(Type<?> type) {
    while (type instanceof NonNullType nonNullType) {
      type = nonNullType.getType();
    }
    while (type instanceof ListType listType) {
      type = listType.getType();
      while (type instanceof NonNullType nonNullType) {
        type = nonNullType.getType();
      }
    }
    return ((TypeName) type).getName();
  }

  private static IllegalArgumentException invalid(String message, Object... args) {
    return new IllegalArgumentException(message.formatted(args));
  }

  private static TypeDefinitionRegistry getTypeDefinitionRegistry(File schemaFile) {
    SchemaParser schemaParser = new SchemaParser();
    TypeDefinitionRegistry typeDefinitionRegistry = new TypeDefinitionRegistry();
    List<File> files = new ArrayList<>();

    typeDefinitionRegistry.merge(schemaParser.parse(schemaFile));

    String rootPackageName = getRootPackageName(typeDefinitionRegistry);
    String typesPath = rootPackageName.replace('.', File.separatorChar);

    File graphqlsDir =
        requireNonNull(schemaFile.getParentFile()).toPath().resolve(typesPath).toFile();

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
        || objectTypeDefinition.hasDirective(Directives.ENTITY_EXTENSION);
  }

  ClassName entityIdClassName(GraphQLTypeName graphQLTypeName) {
    Optional<TypeDefinition> typeDefinition =
        typeDefinitionRegistry.getType(graphQLTypeName.value());
    if (typeDefinition.isEmpty()
        || !(typeDefinition.get() instanceof ObjectTypeDefinition objectTypeDefinition)) {
      throw new IllegalArgumentException("Only ObjectTypeDefinitions can have entity ids");
    }

    Optional<String> composedInEntity =
        getDirectiveArgumentString(
            objectTypeDefinition, Directives.ENTITY_EXTENSION, DirectiveArgs.OF_ENTITY);
    if (composedInEntity.isPresent()) {
      return entityIdClassName(GraphQLTypeName.of(composedInEntity.get()));
    }
    if (objectTypeDefinition.hasDirective(Directives.ENTITY)
        || isSimpleType(objectTypeDefinition)) {
      return ClassName.get(
          getPackageNameForType(graphQLTypeName),
          graphQLTypeName.value() + GraphQlCodeGenUtil.GRAPHQL_ID_SUFFIX);
    }
    throw new IllegalArgumentException("Only object types with an identity can have ids");
  }

  public static GraphQlFieldSpec fieldSpecFromField(
      FieldDefinition fieldDefinition, String nestingPrefix, GraphQLTypeName enclosingType) {
    return GraphQlFieldSpec.builder()
        .fieldName(nestingPrefix + fieldDefinition.getName())
        .fieldDefinition(fieldDefinition)
        .enclosingType(enclosingType)
        .build();
  }

  private static void setFieldVajramsForEachEntity(
      Map<GraphQLTypeName, Map<GraphQlFieldSpec, ClassName>> entityTypeToFieldToTypeAggregator,
      Map<GraphQLTypeName, Map<GraphQlFieldSpec, Fetcher>> entityTypeToFieldToFetcher,
      Map<GraphQLTypeName, Map<Fetcher, List<GraphQlFieldSpec>>> typeToFetcherToFields,
      TypeDefinitionRegistry typeDefinitionRegistry,
      Map<GraphQLTypeName, ObjectTypeDefinition> aggregatableTypes,
      String rootPackageName) {

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
            typeDefinitionRegistry
                .getType(fieldDefinitionType)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Could not find type for field: " + fieldDefinition));

        String path = "";
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          GraphQlFieldSpec fieldSpec = fieldSpecFromField(fieldDefinition, "", parentType);
          boolean multiField = isMultiFieldDataFetcher(fieldDefinition);
          fieldToFetcherMap.put(
              fieldSpec,
              new VajramFetcher(
                  getDataFetcherClassName(fieldDefinition, rootPackageName),
                  multiField ? MULTI_FIELD_DATA_FETCHER : SINGLE_FIELD_DATA_FETCHER));
        } else if (fieldDefinition.hasDirective(Directives.ID_FETCHER)
            && (fieldTypeDefinition.hasDirective(Directives.ENTITY)
                || isSimpleType(fieldTypeDefinition))) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new VajramFetcher(
                  getIdFetcherClassName(fieldDefinition, rootPackageName),
                  GraphQlFetcherType.ID_FETCHER));
          addAggregator(
              fieldDefinition,
              fieldTypeDefinition,
              parentType,
              fieldToTypeAggregator,
              typeDefinitionRegistry,
              rootPackageName);
        } else if (fieldTypeDefinition.hasDirective(Directives.ENTITY)
            && fieldDefinition.hasDirective(Directives.INHERIT_ID_FROM_ARGS)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new SimpleFetcher(GraphQlFetcherType.INHERIT_ID_FROM_ARGS));
          addAggregator(
              fieldDefinition,
              fieldTypeDefinition,
              parentType,
              fieldToTypeAggregator,
              typeDefinitionRegistry,
              rootPackageName);
        } else if (fieldTypeDefinition.hasDirective(Directives.ENTITY_EXTENSION)
            && fieldDefinition.hasDirective(Directives.INHERIT_ID_FROM_PARENT)) {
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new SimpleFetcher(GraphQlFetcherType.INHERIT_ID_FROM_PARENT));
          addAggregator(
              fieldDefinition,
              fieldTypeDefinition,
              parentType,
              fieldToTypeAggregator,
              typeDefinitionRegistry,
              rootPackageName);
        } else {
          dfsSchema(
              fieldDefinition,
              path,
              fieldToFetcherMap,
              typeDefinitionRegistry,
              parentType,
              rootPackageName);
        }
      }

      Map<Fetcher, List<GraphQlFieldSpec>> fetcherToFieldsMap =
          fieldToFetcherMap.entrySet().stream()
              .collect(groupingBy(Entry::getValue, mapping(Entry::getKey, toList())));

      entityTypeToFieldToTypeAggregator.put(parentType, fieldToTypeAggregator);
      entityTypeToFieldToFetcher.put(parentType, fieldToFetcherMap);
      typeToFetcherToFields.put(parentType, fetcherToFieldsMap);
    }
  }

  private static boolean isSimpleType(TypeDefinition<?> typeDefinition) {
    return typeDefinition instanceof ObjectTypeDefinition objectTypeDefinition
        && !objectTypeDefinition.hasDirective(Directives.ENTITY)
        && !objectTypeDefinition.hasDirective(Directives.ENTITY_EXTENSION);
  }

  private static void addAggregator(
      FieldDefinition fieldDefinition,
      TypeDefinition fieldTypeDefinition,
      GraphQLTypeName type,
      Map<GraphQlFieldSpec, ClassName> fieldToTypeAggregator,
      TypeDefinitionRegistry typeDefinitionRegistry,
      String rootPackageName) {
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
      String packageName =
          getPackageNameForType(graphQlTypeName, typeDefinitionRegistry, rootPackageName);
      String typeAggregatorSimpleName =
          graphQlTypeName.value() + Constants.GRAPHQL_AGGREGATOR_SUFFIX;

      fieldToTypeAggregator.put(
          fieldSpecFromField(fieldDefinition, "", type),
          ClassName.get(packageName, typeAggregatorSimpleName));
    } catch (Exception e) {
      // Silently ignore - type might not be an entity
    }
  }

  private static void dfsSchema(
      FieldDefinition incomingField,
      String path,
      Map<GraphQlFieldSpec, Fetcher> fieldToResolverMap,
      TypeDefinitionRegistry typeRegistry,
      GraphQLTypeName enclosingType,
      String rootPackageName) {
    TypeDefinition<?> typeDefinition = typeRegistry.getType(incomingField.getType()).orElseThrow();
    String newPath = path + incomingField.getName() + ".";
    /* Check if its having dataFetcher directive */
    if (typeDefinition instanceof ObjectTypeDefinition typeDefinitionCast
        && typeDefinition.hasDirective(Directives.DATA_FETCHER)) {
      Fetcher baseFetcher =
          new VajramFetcher(
              getDataFetcherClassName(typeDefinition, rootPackageName), MULTI_FIELD_DATA_FETCHER);
      /* Iterate through the children fields and recursively call if they are having dataFetcher */
      for (FieldDefinition fieldDefinition : typeDefinitionCast.getFieldDefinitions()) {
        if (fieldDefinition.hasDirective(Directives.DATA_FETCHER)) {
          fieldToResolverMap.put(
              fieldSpecFromField(fieldDefinition, newPath, enclosingType),
              new VajramFetcher(
                  getDataFetcherClassName(fieldDefinition, rootPackageName),
                  SINGLE_FIELD_DATA_FETCHER));
        } else if (typeRegistry.getType(fieldDefinition.getType()).orElse(null)
            instanceof ObjectTypeDefinition innerFieldTypeDef) {
          if (innerFieldTypeDef.hasDirective(Directives.DATA_FETCHER)) {
            dfsSchema(
                fieldDefinition,
                newPath,
                fieldToResolverMap,
                typeRegistry,
                enclosingType,
                rootPackageName);
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
            new VajramFetcher(
                getDataFetcherClassName(incomingField, rootPackageName),
                SINGLE_FIELD_DATA_FETCHER));
      }
    }
  }

  private static String getPackageNameFromDirective(
      DirectivesContainer<?> directivesContainer,
      @Nullable String directiveName,
      String rootPackageName) {
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
    return getDataFetcherClassName(directivesContainer, rootPackageName);
  }

  public static ClassName getDataFetcherClassName(
      DirectivesContainer<?> directivesContainer, String rootPackageName) {
    String packageName =
        getPackageNameFromDirective(directivesContainer, Directives.DATA_FETCHER, rootPackageName);
    return ClassName.get(
        packageName,
        getDirectiveArgumentString(
                directivesContainer, Directives.DATA_FETCHER, DirectiveArgs.VAJRAM_ID)
            .orElseThrow());
  }

  public static boolean isMultiFieldDataFetcher(DirectivesContainer<?> directivesContainer) {
    List<Directive> directives = directivesContainer.getDirectives(Directives.DATA_FETCHER);
    if (directives.isEmpty()) {
      return false;
    }
    Argument argument = directives.get(0).getArgument(DirectiveArgs.MULTI_FIELD);
    return argument != null
        && argument.getValue() instanceof BooleanValue booleanValue
        && booleanValue.isValue();
  }

  public ClassName getIdFetcherClassName(DirectivesContainer<?> directivesContainer) {
    return getIdFetcherClassName(directivesContainer, rootPackageName);
  }

  private static ClassName getIdFetcherClassName(
      DirectivesContainer<?> directivesContainer, String rootPackageName) {
    return ClassName.get(
        getPackageNameFromDirective(directivesContainer, Directives.ID_FETCHER, rootPackageName),
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
    return getPackageNameForType(graphQLTypeName, typeDefinitionRegistry, rootPackageName);
  }

  static String getPackageNameForType(
      GraphQLTypeName graphQLTypeName,
      TypeDefinitionRegistry typeDefinitionRegistry,
      String rootPackageName) {
    TypeDefinition objectTypeDefinition =
        requireNonNull(
            typeDefinitionRegistry.types().get(graphQLTypeName.value()),
            () -> "Could not find type definition for type: " + graphQLTypeName);
    String subPackage =
        getDirectiveArgumentString(objectTypeDefinition, Directives.SUB_PACKAGE, DirectiveArgs.NAME)
            .orElse(graphQLTypeName.value().toLowerCase(Locale.ROOT))
            .trim();
    if (!subPackage.isEmpty()) {
      subPackage = "." + subPackage;
    }
    return rootPackageName + subPackage;
  }

  public String getEntityIdFieldName(TypeDefinition fieldTypeDef) {
    if (!(fieldTypeDef instanceof ObjectTypeDefinition objectTypeDefinition)) {
      throw new IllegalArgumentException("Only object types can have identity fields");
    }
    List<FieldDefinition> idFields =
        objectTypeDefinition.getFieldDefinitions().stream()
            .filter(field -> field.hasDirective(Directives.ID_FIELD))
            .toList();
    if (idFields.size() != 1) {
      throw new IllegalArgumentException(
          "Type '%s' must declare exactly one @idField when its identity is used"
              .formatted(objectTypeDefinition.getName()));
    }
    return idFields.get(0).getName();
  }

  public Optional<GraphQLTypeName> getComposingEntityType(ObjectTypeDefinition typeDefinition) {
    return typeDefinition.hasDirective(Directives.ENTITY)
        ? Optional.of(GraphQLTypeName.of(typeDefinition))
        : getDirectiveArgumentString(
                typeDefinition, Directives.ENTITY_EXTENSION, DirectiveArgs.OF_ENTITY)
            .map(GraphQLTypeName::of);
  }

  public boolean isOperationType(GraphQLTypeName objectTypeName) {
    String value = objectTypeName.value();

    ObjectTypeDefinition queryType = queryType();
    ObjectTypeDefinition mutationType = mutationType();
    ObjectTypeDefinition subscriptionType = subscriptionType();
    return (queryType != null && value.equals(queryType.getName()))
        || (mutationType != null && value.equals(mutationType.getName()))
        || (subscriptionType != null && value.equals(subscriptionType.getName()));
  }

  public ClassName getAggregatorName(GraphQLTypeName typeName) {
    return ClassName.get(
        getPackageNameForType(typeName), typeName.value() + GRAPHQL_AGGREGATOR_SUFFIX);
  }

  /** Extracts ClassName from a @javaType directive's packageName and className arguments. */
  static @Nullable ClassName extractClassNameFromJavaTypeDirective(
      DirectivesContainer<?> directivesContainer, String contextDescription) {
    List<Directive> javaTypeDirectives = directivesContainer.getDirectives(JAVA_TYPE_DIRECTIVE);
    if (javaTypeDirectives.isEmpty()) {
      return null;
    }

    Directive directive = javaTypeDirectives.get(0);
    Argument packageNameArg = directive.getArgument(PACKAGE_NAME_DIR_ARG);
    Argument classNameArg = directive.getArgument(CLASS_NAME_DIR_ARG);

    if (packageNameArg == null || classNameArg == null) {
      throw new IllegalStateException(
          "%s has @javaType directive without required 'packageName' or 'className' argument"
              .formatted(contextDescription));
    }

    if (!(packageNameArg.getValue() instanceof StringValue packageNameValue)
        || !(classNameArg.getValue() instanceof StringValue classNameValue)) {
      throw new IllegalStateException(
          "%s @javaType directive: 'packageName' and 'className' must be String literals, got packageName: %s, className: %s"
              .formatted(
                  contextDescription,
                  packageNameArg.getValue().getClass().getSimpleName(),
                  classNameArg.getValue().getClass().getSimpleName()));
    }

    return ClassName.get(packageNameValue.getValue(), classNameValue.getValue());
  }

  /** Gets the Java type mapping for a scalar type from the @javaType directive. */
  public @Nullable ClassName getJavaTypeForScalar(String scalarName) {
    return typeDefinitionRegistry
        .getType(scalarName, ScalarTypeDefinition.class)
        .map(
            scalarDef ->
                extractClassNameFromJavaTypeDirective(
                    scalarDef, "Scalar '%s'".formatted(scalarName)))
        .orElse(null);
  }
}
