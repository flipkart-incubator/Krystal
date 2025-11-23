package com.flipkart.krystal.vajram.graphql.api.traits;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_AGGREGATOR_SUFFIX;
import static graphql.language.OperationDefinition.Operation.MUTATION;
import static graphql.language.OperationDefinition.Operation.QUERY;
import static graphql.language.OperationDefinition.Operation.SUBSCRIPTION;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.traits.ComputeDispatchPolicy;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import graphql.ExecutionInput;
import graphql.language.OperationDefinition.Operation;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public abstract class GraphQlOperationDispatch extends ComputeDispatchPolicy {

  private final VajramKryonGraph graph;
  private final TypeDefinitionRegistry typeDefinitionRegistry;

  protected GraphQlOperationDispatch(
      VajramKryonGraph graph, TypeDefinitionRegistry typeDefinitionRegistry) {
    this.graph = graph;
    this.typeDefinitionRegistry = typeDefinitionRegistry;
  }

  @Override
  public VajramID traitID() {
    return graph.getVajramIdByVajramReqType(GraphQlOperationAggregate_Req.class);
  }

  @Override
  public @Nullable VajramID getDispatchTargetID(
      @Nullable Dependency dependency, Request<?> request) {
    if (request instanceof GraphQlOperationAggregate_Req<?> operationAggregateReq) {
      ExecutionInput executionInput = operationAggregateReq.executionInput();
      if (executionInput == null) {
        log.error(
            "Execution input is null - cannot compute dispatch target for {}. Forwarding as is.",
            operationAggregateReq);
        return null;
      }
      Optional<SchemaDefinition> schemaDefinition = typeDefinitionRegistry.schemaDefinition();
      if (schemaDefinition.isEmpty()) {
        log.error(
            """
                Schema definition is empty. \
                SchemaDefinition is mandatory for vajram-graphql - cannot compute dispatch target for {}. \
                Forwarding as is.""",
            operationAggregateReq);
        return null;
      }
      Map<String, OperationTypeDefinition> operationTypesByOpName =
          schemaDefinition.get().getOperationTypeDefinitions().stream()
              .collect(Collectors.toMap(OperationTypeDefinition::getName, op -> op));
      String requestedOperationType = executionInput.getOperationName();
      String resolvedOperationTypeName;
      if (requestedOperationType == null || requestedOperationType.isBlank()) {
        Operation operationType = operationAggregateReq.operationType();
        if (operationType == null) {
          log.error(
              "Operation type is null - cannot compute dispatch target for {}. Forwarding as is.",
              operationAggregateReq);
          return null;
        }
        if (QUERY.equals(operationType)) {
          OperationTypeDefinition queryOperationDef = operationTypesByOpName.get("query");
          if (queryOperationDef == null) {
            log.error(
                "Default query operation has not been configured in schema {} - cannot compute dispatch target for {}. Forwarding as is.",
                operationAggregateReq);
            return null;
          }
          resolvedOperationTypeName = queryOperationDef.getTypeName().getName();
        } else if (MUTATION.equals(operationType)) {
          OperationTypeDefinition mutationOperationDef = operationTypesByOpName.get("mutation");
          if (mutationOperationDef == null) {
            log.error(
                "Default mutation operation has not been configured in schema {} - cannot compute dispatch target for {}. Forwarding as is.",
                schemaDefinition);
            return null;
          }
          resolvedOperationTypeName = mutationOperationDef.getTypeName().getName();
        } else if (SUBSCRIPTION.equals(operationType)) {
          OperationTypeDefinition subscriptionOperationDef =
              operationTypesByOpName.get("subscription");
          if (subscriptionOperationDef == null) {
            log.error(
                "Default subscription operation has not been configured in schema {} - cannot compute dispatch target for {}. Forwarding as is.",
                schemaDefinition);
            return null;
          }
          resolvedOperationTypeName = subscriptionOperationDef.getTypeName().getName();
        } else {
          throw new UnsupportedOperationException("Unrecognized operation type: " + operationType);
        }
      } else {
        resolvedOperationTypeName = requestedOperationType;
      }
      return vajramID(resolvedOperationTypeName + GRAPHQL_AGGREGATOR_SUFFIX);
    }
    return request._vajramID();
  }
}
