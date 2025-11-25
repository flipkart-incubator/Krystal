package com.flipkart.krystal.vajram.graphql.api.execution;

import static com.flipkart.krystal.data.RequestResponseFuture.forRequest;
import static graphql.ExecutionResult.newExecutionResult;
import static graphql.execution.ExecutionStrategyParameters.newParameters;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;

import com.flipkart.krystal.core.VajramInvocation;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import com.flipkart.krystal.vajram.graphql.api.model.GraphQlOperationObject;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_Req;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationAggregate_ReqImmutPojo;
import com.flipkart.krystal.vajram.graphql.api.traits.GraphQlOperationDispatch;
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
import graphql.language.OperationDefinition.Operation;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLType;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This execution strategy is responsible for executing a graphql operation by setting the following
 * fields in the {@link GraphQlOperationAggregate_Req} object.
 *
 * <ul>
 *   <li>operationType
 *   <li>graphql_executionContext
 *   <li>graphql_executionStrategy
 *   <li>graphql_executionStrategyParams
 * </ul>
 *
 * These fields are subsequently used by the {@link GraphQlOperationDispatch} dependency decorator
 * to dispatch the call to the appropriate vajram implementing the {@link GraphQlOperationAggregate}
 * trait.
 */
@Singleton
public final class VajramExecutionStrategy extends ExecutionStrategy {

  public static final Class<VajramInvocation> VAJRAM_INVOCATION_CTX_KEY = VajramInvocation.class;
  public static final Class<GraphQlOperationAggregate_Req> GRAPHQL_OP_AGGREGATE_REQUEST =
      GraphQlOperationAggregate_Req.class;

  private final FieldCollector fieldCollector = new FieldCollector();

  @Inject
  public VajramExecutionStrategy() {}

  @Override
  public CompletableFuture<ExecutionResult> execute(
      ExecutionContext executionContext, ExecutionStrategyParameters parameters)
      throws NonNullableFieldWasNullException {
    GraphQLContext graphQLContext = executionContext.getGraphQLContext();
    Operation operation = executionContext.getOperationDefinition().getOperation();
    GraphQlOperationAggregate_Req<GraphQlOperationObject> graphQlOperationObjectBuilder =
        requireNonNullElseGet(
            graphQLContext.get(GRAPHQL_OP_AGGREGATE_REQUEST),
            () ->
                GraphQlOperationAggregate_ReqImmutPojo._builder()
                    .executionInput(executionContext.getExecutionInput()));
    GraphQlOperationAggregate_Req<GraphQlOperationObject> request =
        graphQlOperationObjectBuilder
            ._asBuilder()
            .operationType(operation)
            .graphql_executionContext(executionContext)
            .graphql_executionStrategy(this)
            .graphql_executionStrategyParams(parameters);

    VajramInvocation<GraphQlOperationObject> vajramInvocation =
        requireNonNull(
            graphQLContext.get(VAJRAM_INVOCATION_CTX_KEY), "VajramInvocation cannot be null");
    RequestResponseFuture<Request<GraphQlOperationObject>, GraphQlOperationObject>
        requestResponseFuture = forRequest(request);
    vajramInvocation.executeVajram(requestResponseFuture);
    return requestResponseFuture
        .response()
        // Wrap the response in GraphQlObjectResult so that it can be extracted and converted to a
        // proper complete ExecutionResult if and when needed
        .handle((r, e) -> newExecutionResult().data(new GraphQlObjectResult(r, e)).build());
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
