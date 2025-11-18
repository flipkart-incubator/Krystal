package com.flipkart.krystal.vajram.graphql.api;

import static com.flipkart.krystal.vajram.graphql.api.QueryAnalyseUtil.getNodeExecutionConfigBasedOnQuery;
import static graphql.execution.ExecutionStrategyParameters.newParameters;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.vajram.graphql.api.ExecutionLifecycleListener.ExecutionStartEvent;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlTypeModel_Immut;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
import graphql.GraphQLError;
import graphql.GraphqlErrorException;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.ExecutionStrategy;
import graphql.execution.ExecutionStrategyParameters;
import graphql.execution.ExecutionStrategyParameters.Builder;
import graphql.execution.FieldCollector;
import graphql.execution.FieldCollectorParameters;
import graphql.execution.MergedField;
import graphql.execution.MergedSelectionSet;
import graphql.execution.NonNullableFieldValidator;
import graphql.execution.NonNullableFieldWasNullException;
import graphql.execution.ResultPath;
import graphql.normalized.ExecutableNormalizedField;
import graphql.normalized.ExecutableNormalizedOperation;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO: remove this once it's moved to graphQlJava, did it due to
//  [array] -> [array] field support in schema resolution support
public class VajramExecutionStrategy extends ExecutionStrategy {

  public static final String VAJRAM_KRYON_GRAPH = "vajram_kryon_graph";
  public static final String VAJRAM_EXECUTOR_CONFIG = "vajram_executor_config";
  public static final String TASK_ID = "taskId";
  public static final String TYPENAME_FIELD = "__typename";
  private final FieldCollector fieldCollector = new FieldCollector();
  private final InitVajramRequestCreator initVajramRequestCreator;
  private final ExecutionLifecycleListener executionLifecycleListener;
  private final Map<String, Map<String, List<String>>> reverseEntityTypeToFieldResolverMap;
  private final Map<String, Map<String, String>> entityToRefToTypeMap;
  private final Map<String, Map<String, String>> entityTypeToReferenceFetcher;

  public VajramExecutionStrategy(
      InitVajramRequestCreator initVajramRequestCreator,
      ExecutionLifecycleListener executionLifecycleListener,
      Map<String, Map<String, List<String>>> reverseEntityTypeToFieldResolverMap,
      Map<String, Map<String, String>> entityToRefToTypeMap,
      Map<String, Map<String, String>> entityTypeToReferenceFetcher) {
    this.initVajramRequestCreator = initVajramRequestCreator;
    this.executionLifecycleListener = executionLifecycleListener;
    this.reverseEntityTypeToFieldResolverMap = reverseEntityTypeToFieldResolverMap;
    this.entityToRefToTypeMap = entityToRefToTypeMap;
    this.entityTypeToReferenceFetcher = entityTypeToReferenceFetcher;
  }

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext executionContext, ExecutionStrategyParameters parameters)
      throws NonNullableFieldWasNullException {
    GraphQLContext graphQLContext = executionContext.getGraphQLContext();
    KrystexVajramExecutorConfigBuilder vajramExecutorConfigBuilder =
        graphQLContext.getOrDefault(VAJRAM_EXECUTOR_CONFIG, KrystexVajramExecutorConfig.builder());
    VajramKryonGraph vajramKryonGraph = graphQLContext.get(VAJRAM_KRYON_GRAPH);

    ImmutableSet<DependentChain> dependantChainList =
        getNodeExecutionConfigBasedOnQuery(
            executionContext,
            reverseEntityTypeToFieldResolverMap,
            entityToRefToTypeMap,
            entityTypeToReferenceFetcher,
            vajramKryonGraph);

    ImmutableRequest<?> immutableRequest =
        initVajramRequestCreator.create(executionContext, this, getParams(parameters));
    try (KrystexVajramExecutor vajramExecutor =
        KrystexVajramExecutor.builder()
            .vajramKryonGraph(vajramKryonGraph)
            .executorConfig(vajramExecutorConfigBuilder.build())
            .build()) {
      executionLifecycleListener.onExecutionStart(
          new ExecutionStartEvent(vajramExecutor, executionContext));
      return vajramExecutor
          .execute(
              immutableRequest,
              KryonExecutionConfig.builder()
                  .disabledDependentChains(dependantChainList)
                  .executionId(graphQLContext.get(TASK_ID))
                  .build())
          .handle(
              (o, throwable) -> {
                if (throwable != null) {
                  return ExecutionResult.newExecutionResult()
                      .addError(GraphqlErrorException.newErrorException().cause(throwable).build())
                      .build();
                } else {
                  Object resultData = processTypenameFields(executionContext, o);

                  // Collect errors from the result if it's a GraphQL type model
                  var resultBuilder = ExecutionResult.newExecutionResult().data(resultData);

                  if (resultData instanceof GraphQlTypeModel_Immut model) {

                    // Use ErrorCollector to gather all errors
                    ErrorCollector errorCollector = new DefaultErrorCollector();
                    model._collectErrors(errorCollector, new java.util.ArrayList<>());

                    // Add collected errors to the execution result
                    List<GraphQLError> errors = errorCollector.getErrors();
                    if (!errors.isEmpty()) {
                      resultBuilder.addErrors(errors);
                    }
                  }

                  return resultBuilder.build();
                }
              });
    }
  }

  /**
   * Process the execution result to include __typename fields
   *
   * @return The processed data including __typename fields
   */
  private Object processTypenameFields(ExecutionContext executionContext, Object originalData) {
    // Data coming after the processing of remaining fields is itself null
    if (originalData == null) {
      return null;
    }

    // Get metadata fields set from context
    Set<String> metadataFields =
        executionContext.getGraphQLContext().get(QueryAnalyseUtil.METADATA_FIELDS);
    if (metadataFields == null || metadataFields.isEmpty()) {
      return originalData;
    }

    if (metadataFields.contains(TYPENAME_FIELD)) {
      addTypeNameFieldsToObject(originalData, executionContext);
    }

    return originalData;
  }

  /** Add typename field to objects based on the GraphQL query */
  private void addTypeNameFieldsToObject(Object originalData, ExecutionContext executionContext) {
    if (originalData == null) {
      return;
    }

    // Get normalized query to analyze field selections
    ExecutableNormalizedOperation operation = executionContext.getNormalizedQueryTree().get();
    Collection<ExecutableNormalizedField> fields = operation.getTopLevelFields();

    // Recursively go through the GraphQL query structure to identify where __typename was requested
    Map<String, Boolean> fieldPathsWithTypename = new HashMap<>();

    // Specifically check for top-level __typename field first
    for (ExecutableNormalizedField field : fields) {
      if (field.getName().equals(TYPENAME_FIELD)) {
        fieldPathsWithTypename.put("", true);
        break;
      }
    }

    // Search for typename in remaining fields
    for (ExecutableNormalizedField field : fields) {
      if (!field.getName().equals(TYPENAME_FIELD)) {
        populateFieldPathsWithTypename(field, "", fieldPathsWithTypename);
      }
    }

    // Add __typename to objects based on the mapping
    if (!fieldPathsWithTypename.isEmpty()) {
      addTypenameToObjectsBasedOnPaths(originalData, "", fieldPathsWithTypename);
    }
  }

  /** Populate a map of field paths that have __typename requested */
  private void populateFieldPathsWithTypename(
      ExecutableNormalizedField field, String path, Map<String, Boolean> pathsWithTypename) {

    // Check for __typename at the current level
    boolean hasTypenameField = false;
    String currentPath = path.isEmpty() ? field.getName() : path + "." + field.getName();

    for (ExecutableNormalizedField child : field.getChildren()) {
      if (child.getName().equals(TYPENAME_FIELD)) {
        hasTypenameField = true;
        break;
      }
    }

    if (hasTypenameField) {
      pathsWithTypename.put(currentPath, true);
    }

    // Check for typename in children levels
    for (ExecutableNormalizedField child : field.getChildren()) {
      if (!child.getName().equals(TYPENAME_FIELD)) {
        populateFieldPathsWithTypename(child, currentPath, pathsWithTypename);
      }
    }
  }

  /** Add typename to objects based on paths from the query */
  private void addTypenameToObjectsBasedOnPaths(
      Object obj, String currentPath, Map<String, Boolean> pathsWithTypename) {

    if (obj == null) {
      return;
    }

    // When GraphQlModel Objects are queried
    if (obj instanceof AbstractGraphQlModel<?> model) {

      // Check if this path should have __typename
      if (pathsWithTypename.containsKey(currentPath)) {
        model._values().put(TYPENAME_FIELD, model.getClass().getSimpleName());
      }

      // Process child objects
      for (Map.Entry<String, Object> entry : new HashMap<>(model._values()).entrySet()) {
        String fieldName = entry.getKey();
        Object value = entry.getValue();

        // Skip typename field
        if (!fieldName.equals(TYPENAME_FIELD)) {
          String childPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
          addTypenameToObjectsBasedOnPaths(value, childPath, pathsWithTypename);
        }
      }
    }
    // For lists, process each item with the same path
    else if (obj instanceof List<?> list) {
      for (Object item : list) {
        addTypenameToObjectsBasedOnPaths(item, currentPath, pathsWithTypename);
      }
    }
    // For maps, process values
    else if (obj instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) obj;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        String fieldName = entry.getKey();
        Object value = entry.getValue();

        // Skip typename field
        if (!fieldName.equals(TYPENAME_FIELD)) {
          String childPath = currentPath.isEmpty() ? fieldName : currentPath + "." + fieldName;
          addTypenameToObjectsBasedOnPaths(value, childPath, pathsWithTypename);
        }
      }
    }
  }

  /**
   * Transform the parameters by setting the field and path correctly. We need to handle the case
   * where __typename interferes with normal field selection.
   */
  private ExecutionStrategyParameters getParams(ExecutionStrategyParameters parameters) {
    MergedSelectionSet fields = parameters.getFields();
    List<String> fieldNames = fields.getKeys();

    if (!fieldNames.isEmpty()) {
      // If __typename is present with other fields, prioritize the substantive fields
      if (fieldNames.contains(TYPENAME_FIELD) && fieldNames.size() > 1) {
        // Get a non-typename field to use as the current field
        String firstSubstantiveField =
            fieldNames.stream()
                .filter(name -> !name.equals(TYPENAME_FIELD))
                .findFirst()
                .orElse(fieldNames.get(0));

        MergedField currentField = fields.getSubField(firstSubstantiveField);
        ResultPath fieldPath = parameters.getPath().segment(mkNameForPath(currentField));

        return parameters.transform(
            builder -> builder.field(currentField).path(fieldPath).parent(parameters));
      } else {
        // Standard case - use first field
        String firstField = fieldNames.get(0);
        MergedField currentField = fields.getSubField(firstField);
        ResultPath fieldPath = parameters.getPath().segment(mkNameForPath(currentField));

        return parameters.transform(
            builder -> builder.field(currentField).path(fieldPath).parent(parameters));
      }
    }
    return parameters;
  }

  public ExecutionStrategyParameters newParametersForFieldExecution(
      ExecutionContext executionContext,
      ExecutionStrategyParameters parameters,
      @Nullable MergedField field) {
    ResultPath resultPath = parameters.getPath();
    if (field != null) {
      resultPath = parameters.getPath().segment(mkNameForPath(field));
    }
    Builder builder = newParameters(parameters).path(resultPath);
    if (field != null) {
      builder.field(field);
    }
    ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();
    GraphQLObjectType resolvedObjectType =
        resolveType(executionContext, parameters, executionStepInfo.getUnwrappedNonNullType());
    ExecutionStepInfo newExecutionStepInfo =
        executionStepInfo.changeTypeWithPreservedNonNull(resolvedObjectType);
    NonNullableFieldValidator nonNullableFieldValidator =
        new NonNullableFieldValidator(executionContext);
    builder
        .executionStepInfo(newExecutionStepInfo)
        .nonNullFieldValidator(nonNullableFieldValidator)
        .source(executionContext.getValueUnboxer().unbox(parameters.getSource()))
        .parent(parameters);
    FieldCollectorParameters collectorParameters =
        FieldCollectorParameters.newParameters()
            .schema(executionContext.getGraphQLSchema())
            .objectType(resolvedObjectType)
            .fragments(executionContext.getFragmentsByName())
            .variables(executionContext.getCoercedVariables().toMap())
            .build();

    ExecutionStrategyParameters newParameters = builder.build();
    MergedSelectionSet subFields;

    if (field != null) {
      subFields = fieldCollector.collectFields(collectorParameters, newParameters.getField());
      newParameters =
          newParameters(newParameters)
              .executionStepInfo(
                  createExecutionStepInfo(
                      executionContext,
                      newParameters,
                      getFieldDef(executionContext, newParameters, field.getSingleField()),
                      resolvedObjectType))
              .build();
      return newParameters(newParameters).fields(subFields).build();
    } else {
      return newParameters(newParameters).build();
    }
  }

  /**
   * Copied from {@link ExecutionStrategy#mkNameForPath(MergedField)} as that is {@link
   * graphql.Internal}
   */
  public static String mkNameForPath(MergedField mergedField) {
    return mergedField.getFields().get(0).getResultKey();
  }

  @Override
  protected GraphQLObjectType resolveType(
      ExecutionContext executionContext,
      ExecutionStrategyParameters parameters,
      GraphQLType fieldType) {
    if (fieldType instanceof GraphQLList graphQLList) {
      fieldType = graphQLList.getWrappedType();
    }
    return super.resolveType(executionContext, parameters, fieldType);
  }

  @FunctionalInterface
  public interface InitVajramRequestCreator {
    ImmutableRequest<?> create(
        ExecutionContext graphQLExecutionContext,
        VajramExecutionStrategy graphQLExecutionStrategy,
        ExecutionStrategyParameters graphQLExecutionStrategyParameters);
  }
}
