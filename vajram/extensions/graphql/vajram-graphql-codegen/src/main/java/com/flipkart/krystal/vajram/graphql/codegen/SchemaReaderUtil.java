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
import graphql.language.EnumValue;
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
import java.util.List;
import java.util.Locale;
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

  @Getter private final Map<GraphQLTypeName, @NonNull ObjectTypeDefinition> composedOnlyTypes;

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
    this.composedOnlyTypes =
        Maps.filterValues(
            graphQLObjectTypes, typeDef -> typeDef.hasDirective(Directives.COMPOSED_ONLY));

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
    validateDirectiveInvariants(typeDefinitionRegistry, graphQLObjectTypes, composedOnlyTypes);
    setFieldVajramsForEachType(
        entityTypeToFieldToTypeAggregator,
        entityTypeToFieldToFetcher,
        typeToFetcherToFields,
        typeDefinitionRegistry,
        aggregatableTypes,
        rootPackageName);
  }

  private static void validateDirectiveInvariants(
      TypeDefinitionRegistry typeDefinitionRegistry,
      Map<GraphQLTypeName, ObjectTypeDefinition> graphQLObjectTypes,
      Map<GraphQLTypeName, ObjectTypeDefinition> composedOnlyTypes) {
    graphQLObjectTypes.forEach(
        (typeName, typeDefinition) -> {
          for (FieldDefinition field : typeDefinition.getFieldDefinitions()) {
            TypeDefinition<?> fieldType = typeDefinitionFor(typeDefinitionRegistry, field);
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
              validateInferIdFromArgs(typeDefinitionRegistry, typeName, field, fieldType);
            }
            if (field.hasDirective(Directives.INHERIT_ID_FROM_PARENT)) {
              validateInferIdFromParent(
                  typeDefinitionRegistry, typeName, typeDefinition, field, fieldType);
            }
          }
          // @composedOnly types reuse their root type's identity, and root operation types
          // (Query/Mutation/Subscription) have no identity of their own - every other type must
          // declare at least one non-null @idField to be unambiguously identifiable.
          if (!typeDefinition.hasDirective(Directives.COMPOSED_ONLY)
              && !isOperationType(typeDefinitionRegistry, typeName)
              && idFields(typeDefinition).stream()
                  .noneMatch(idField -> idField.getType() instanceof NonNullType)) {
            throw invalid(
                "Type '%s' must declare at least one non-null @idField", typeName.value());
          }
        });
    validateOperationTypes(typeDefinitionRegistry, graphQLObjectTypes);
    validateComposedOnlyReferences(typeDefinitionRegistry, graphQLObjectTypes, composedOnlyTypes);
  }

  private static void validateInferIdFromArgs(
      TypeDefinitionRegistry typeDefinitionRegistry,
      GraphQLTypeName parentType,
      FieldDefinition field,
      TypeDefinition<?> fieldType) {
    if (!isOperationType(typeDefinitionRegistry, parentType)) {
      throw invalid(
          "@inferIdFromArgs '%s.%s' is only valid on root operation fields",
          parentType.value(), field.getName());
    }
    if (!(fieldType instanceof ObjectTypeDefinition objectType)) {
      throw invalid(
          "@inferIdFromArgs '%s.%s' must return an object type",
          parentType.value(), field.getName());
    }
    if (!hasOwnIdentity(typeDefinitionRegistry, objectType)) {
      throw invalid(
          "@inferIdFromArgs '%s.%s' must return a type with one or more @idField fields",
          parentType.value(), field.getName());
    }
    Map<String, InputValueDefinition> argsByName =
        field.getInputValueDefinitions().stream()
            .collect(Collectors.toMap(InputValueDefinition::getName, input -> input));
    for (FieldDefinition idField : idFields(objectType)) {
      boolean idFieldMandatory = idField.getType() instanceof NonNullType;
      InputValueDefinition arg = argsByName.get(idField.getName());
      if (arg == null) {
        // An optional @idField can be left unmapped; a mandatory one cannot.
        if (idFieldMandatory) {
          throw invalid(
              "@inferIdFromArgs '%s.%s' requires an argument '%s' with type '%s' for @idField '%s.%s'",
              parentType.value(),
              field.getName(),
              idField.getName(),
              idField.getType(),
              objectType.getName(),
              idField.getName());
        }
        continue;
      }
      if (!unwrapNonNull(arg.getType())
          .toString()
          .equals(unwrapNonNull(idField.getType()).toString())) {
        throw invalid(
            "@inferIdFromArgs '%s.%s' requires an argument '%s' with type '%s' for @idField '%s.%s'",
            parentType.value(),
            field.getName(),
            idField.getName(),
            idField.getType(),
            objectType.getName(),
            idField.getName());
      }
      boolean argMandatory = arg.getType() instanceof NonNullType;
      if (idFieldMandatory && !argMandatory) {
        throw invalid(
            "@inferIdFromArgs '%s.%s' requires argument '%s' to be mandatory, since @idField '%s.%s' is mandatory",
            parentType.value(),
            field.getName(),
            idField.getName(),
            objectType.getName(),
            idField.getName());
      }
    }
  }

  private static void validateInferIdFromParent(
      TypeDefinitionRegistry typeDefinitionRegistry,
      GraphQLTypeName parentType,
      ObjectTypeDefinition parent,
      FieldDefinition field,
      TypeDefinition<?> fieldType) {
    if (isOperationType(typeDefinitionRegistry, parentType)) {
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
        || !extension.hasDirective(Directives.COMPOSED_ONLY)) {
      throw invalid(
          "@inferIdFromParent '%s.%s' must return an @composedOnly type",
          parentType.value(), field.getName());
    }
    if (!getRootIdentityType(typeDefinitionRegistry, parent)
        .equals(getRootIdentityType(typeDefinitionRegistry, extension))) {
      throw invalid(
          "@inferIdFromParent '%s.%s' must reference an @composedOnly type in root '%s'",
          parentType.value(),
          field.getName(),
          getRootIdentityType(typeDefinitionRegistry, parent)
              .map(GraphQLTypeName::value)
              .orElse(parentType.value()));
    }
  }

  private static void validateOperationTypes(
      TypeDefinitionRegistry typeDefinitionRegistry,
      Map<GraphQLTypeName, ObjectTypeDefinition> graphQLObjectTypes) {
    typeDefinitionRegistry
        .schemaDefinition()
        .orElseThrow()
        .getOperationTypeDefinitions()
        .forEach(
            operation -> {
              String expectedOperation = operation.getName();
              GraphQLTypeName typeName = GraphQLTypeName.of(operation.getTypeName().getName());
              ObjectTypeDefinition type = graphQLObjectTypes.get(typeName);
              if (type == null) {
                throw invalid(
                    "Schema %s type '%s' must be an object type",
                    expectedOperation, typeName.value());
              }
              List<Directive> directives = type.getDirectives(Directives.OPERATION);
              if (directives.size() != 1) {
                throw invalid(
                    "Schema %s type '%s' must declare exactly one @operation(type: %s)",
                    expectedOperation,
                    typeName.value(),
                    expectedOperation.toUpperCase(Locale.ROOT));
              }
              Argument typeArgument = directives.get(0).getArgument(DirectiveArgs.TYPE);
              if (!(typeArgument.getValue() instanceof EnumValue enumValue)
                  || !expectedOperation.equals(enumValue.getName().toLowerCase(Locale.ROOT))) {
                throw invalid(
                    "Schema %s type '%s' must declare @operation(type: %s)",
                    expectedOperation,
                    typeName.value(),
                    expectedOperation.toUpperCase(Locale.ROOT));
              }
            });
  }

  private static void validateComposedOnlyReferences(
      TypeDefinitionRegistry typeDefinitionRegistry,
      Map<GraphQLTypeName, ObjectTypeDefinition> graphQLObjectTypes,
      Map<GraphQLTypeName, ObjectTypeDefinition> composedOnlyTypes) {
    composedOnlyTypes.forEach(
        (composedTypeName, composedType) -> {
          GraphQLTypeName rootType =
              composedOnlyInRootType(composedType)
                  .orElseThrow(
                      () ->
                          invalid(
                              "@composedOnly '%s' must declare inRootType",
                              composedTypeName.value()));
          ObjectTypeDefinition root = graphQLObjectTypes.get(rootType);
          if (root == null
              || root.hasDirective(Directives.COMPOSED_ONLY)
              || isOperationType(typeDefinitionRegistry, rootType)
              || !hasOwnIdentity(typeDefinitionRegistry, root)) {
            throw invalid(
                "@composedOnly '%s' must reference an identity-owning non-operation root type with inRootType, found '%s'",
                composedTypeName.value(), rootType.value());
          }
        });
    graphQLObjectTypes.forEach(
        (parentName, parent) -> {
          for (FieldDefinition field : parent.getFieldDefinitions()) {
            ObjectTypeDefinition composedType =
                typeDefinitionFor(typeDefinitionRegistry, field)
                            instanceof ObjectTypeDefinition objectType
                        && objectType.hasDirective(Directives.COMPOSED_ONLY)
                    ? objectType
                    : null;
            if (composedType == null) {
              continue;
            }
            if (isOperationType(typeDefinitionRegistry, parentName)
                || getRootIdentityType(typeDefinitionRegistry, parent).isEmpty()) {
              throw invalid(
                  "Field '%s.%s' returns an @composedOnly type but its parent has no identity root",
                  parentName.value(), field.getName());
            }
            if (isList(field.getType())) {
              throw invalid(
                  "Field '%s.%s' returning an @composedOnly type cannot be a list",
                  parentName.value(), field.getName());
            }
            GraphQLTypeName expectedRoot =
                getRootIdentityType(typeDefinitionRegistry, parent).orElseThrow();
            GraphQLTypeName composedRoot = composedOnlyInRootType(composedType).orElseThrow();
            if (!expectedRoot.equals(composedRoot)) {
              throw invalid(
                  "Field '%s.%s' must return an @composedOnly type in root '%s', but '%s' declares root '%s'",
                  parentName.value(),
                  field.getName(),
                  expectedRoot.value(),
                  composedType.getName(),
                  composedRoot.value());
            }
          }
        });
  }

  private static Optional<GraphQLTypeName> composedOnlyInRootType(
      ObjectTypeDefinition composedType) {
    return getDirectiveArgumentString(
            composedType, Directives.COMPOSED_ONLY, DirectiveArgs.IN_ROOT_TYPE)
        .map(GraphQLTypeName::of);
  }

  private static TypeDefinition<?> typeDefinitionFor(
      TypeDefinitionRegistry typeDefinitionRegistry, FieldDefinition field) {
    return typeDefinitionRegistry
        .getType(field.getType())
        .orElseThrow(() -> invalid("Could not find type for field '%s'", field));
  }

  private static List<FieldDefinition> idFields(ObjectTypeDefinition type) {
    return type.getFieldDefinitions().stream()
        .filter(field -> field.hasDirective(Directives.ID_FIELD))
        .toList();
  }

  private static Type<?> unwrapNonNull(Type<?> type) {
    return type instanceof NonNullType nonNullType ? nonNullType.getType() : type;
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

  boolean hasObjectId(TypeDefinition typeDefinition) {
    return hasObjectId(typeDefinitionRegistry, typeDefinition);
  }

  private static boolean hasObjectId(
      TypeDefinitionRegistry typeDefinitionRegistry, TypeDefinition typeDefinition) {
    return typeDefinition instanceof ObjectTypeDefinition objectTypeDefinition
        && getRootIdentityType(typeDefinitionRegistry, objectTypeDefinition).isPresent();
  }

  ClassName entityIdClassName(GraphQLTypeName graphQLTypeName) {
    Optional<TypeDefinition> typeDefinition =
        typeDefinitionRegistry.getType(graphQLTypeName.value());
    if (typeDefinition.isEmpty()
        || !(typeDefinition.get() instanceof ObjectTypeDefinition objectTypeDefinition)) {
      throw new IllegalArgumentException("Only ObjectTypeDefinitions can have entity ids");
    }

    Optional<GraphQLTypeName> rootIdentityType = getRootIdentityType(objectTypeDefinition);
    if (rootIdentityType.isEmpty()) {
      throw new IllegalArgumentException("Only object types with an identity can have ids");
    }
    if (!rootIdentityType.get().equals(graphQLTypeName)) {
      return entityIdClassName(rootIdentityType.get());
    }
    if (hasOwnIdentity(objectTypeDefinition)) {
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

  private static void setFieldVajramsForEachType(
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
      /* Two fields cannot use the same idFetcher vajramId within the same GraphQL type */
      Map<ClassName, FieldDefinition> idFetcherClassNameToField = new HashMap<>();

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
            && hasObjectId(typeDefinitionRegistry, fieldTypeDefinition)) {
          ClassName idFetcherClassName = getIdFetcherClassName(fieldDefinition, rootPackageName);
          FieldDefinition existingField =
              idFetcherClassNameToField.putIfAbsent(idFetcherClassName, fieldDefinition);
          if (existingField != null) {
            throw new IllegalStateException(
                "GraphQL type '%s' has fields '%s' and '%s' both using the same @%s vajramId '%s'. "
                        .formatted(
                            parentType.value(),
                            existingField.getName(),
                            fieldDefinition.getName(),
                            Directives.ID_FETCHER,
                            idFetcherClassName.simpleName())
                    + "A given idFetcher vajramId can only be used by one field per type.");
          }
          fieldToFetcherMap.put(
              fieldSpecFromField(fieldDefinition, "", parentType),
              new VajramFetcher(idFetcherClassName, GraphQlFetcherType.ID_FETCHER));
          addAggregator(
              fieldDefinition,
              fieldTypeDefinition,
              parentType,
              fieldToTypeAggregator,
              typeDefinitionRegistry,
              rootPackageName);
        } else if (hasObjectId(typeDefinitionRegistry, fieldTypeDefinition)
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
        } else if (fieldTypeDefinition.hasDirective(Directives.COMPOSED_ONLY)
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

  public Optional<GraphQLTypeName> getRootIdentityType(ObjectTypeDefinition typeDefinition) {
    return getRootIdentityType(typeDefinitionRegistry, typeDefinition);
  }

  private static Optional<GraphQLTypeName> getRootIdentityType(
      TypeDefinitionRegistry typeDefinitionRegistry, ObjectTypeDefinition typeDefinition) {
    if (typeDefinition.hasDirective(Directives.COMPOSED_ONLY)) {
      return composedOnlyInRootType(typeDefinition);
    }
    return hasOwnIdentity(typeDefinitionRegistry, typeDefinition)
        ? Optional.of(GraphQLTypeName.of(typeDefinition))
        : Optional.empty();
  }

  boolean hasOwnIdentity(ObjectTypeDefinition typeDefinition) {
    return hasOwnIdentity(typeDefinitionRegistry, typeDefinition);
  }

  private static boolean hasOwnIdentity(
      TypeDefinitionRegistry typeDefinitionRegistry, ObjectTypeDefinition typeDefinition) {
    return !typeDefinition.hasDirective(Directives.COMPOSED_ONLY)
        && !isOperationType(typeDefinitionRegistry, GraphQLTypeName.of(typeDefinition))
        && typeDefinition.getFieldDefinitions().stream()
            .anyMatch(field -> field.hasDirective(Directives.ID_FIELD));
  }

  public boolean isOperationType(GraphQLTypeName objectTypeName) {
    return isOperationType(typeDefinitionRegistry, objectTypeName);
  }

  private static boolean isOperationType(
      TypeDefinitionRegistry typeDefinitionRegistry, GraphQLTypeName objectTypeName) {
    return typeDefinitionRegistry
        .getType(objectTypeName.value(), ObjectTypeDefinition.class)
        .map(typeDefinition -> typeDefinition.hasDirective(Directives.OPERATION))
        .orElse(false);
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
