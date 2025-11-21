package com.flipkart.krystal.vajram.graphql.api.execution;

import static com.flipkart.krystal.data.Errable.errableFrom;
import static com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject._asExecutionResult;
import static graphql.execution.ExecutionStrategyParameters.newParameters;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlQueryAggregate_Req;
import graphql.ExecutionResult;
import graphql.GraphQLContext;
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
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

@Singleton
public class VajramExecutionStrategy extends ExecutionStrategy {

  public static final String GRAPHQL_OPERATION_REQUEST_CTX_KEY = "vajram_graphql_operation_request";
  public static final Class<VajramInvocation> VAJRAM_INVOCATION_CTX_KEY = VajramInvocation.class;

  private final FieldCollector fieldCollector = new FieldCollector();

  @Inject
  public VajramExecutionStrategy() {}

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext executionContext, ExecutionStrategyParameters parameters)
      throws NonNullableFieldWasNullException {
    GraphQLContext graphQLContext = executionContext.getGraphQLContext();

    Object operationRequest =
        requireNonNull(
            graphQLContext.get(GRAPHQL_OPERATION_REQUEST_CTX_KEY),
            GRAPHQL_OPERATION_REQUEST_CTX_KEY + " cannot be null");
    VajramInvocation<GraphQlOperationObject> vajramInvocation =
        requireNonNull(
            graphQLContext.get(VAJRAM_INVOCATION_CTX_KEY), "VajramInvocation cannot be null");

    Request<GraphQlOperationObject> request;
    if (operationRequest instanceof GraphQlQueryAggregate_Req<?> queryAggregateReq) {
      //noinspection unchecked
      request =
          (Request<GraphQlOperationObject>)
              queryAggregateReq
                  ._asBuilder()
                  .graphql_executionContext(executionContext)
                  .graphql_executionStrategy(this)
                  .graphql_executionStrategyParams(parameters);
    } else {
      throw new IllegalArgumentException(
          "Operation request is not of type GraphQlQueryAggregate_Req");
    }
    RequestResponseFuture<Request<GraphQlOperationObject>, GraphQlOperationObject>
        requestResponseFuture = new RequestResponseFuture<>(request, new CompletableFuture<>());
    vajramInvocation.executeVajram(requestResponseFuture);
    return requestResponseFuture
        .response()
        .handle(
            (graphQlOpTypeModel, throwable) ->
                _asExecutionResult(errableFrom(graphQlOpTypeModel, throwable)));
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
}
