package com.flipkart.krystal.vajram.graphql.api.traits;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.vajram.graphql.api.Constants.GRAPHQL_AGGREGATOR_SUFFIX;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.traits.ComputeDispatchPolicy;
import com.google.common.collect.ImmutableSet;
import graphql.language.OperationDefinition.Operation;
import graphql.language.OperationTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

@Slf4j
public final class GraphQlOperationDispatch extends ComputeDispatchPolicy {

  private final VajramGraph graph;
  private final TypeDefinitionRegistry typeDefinitionRegistry;
  private final ImmutableSet<Class<? extends Request<?>>> dispatchTargets;
  private final ImmutableSet<VajramID> dispatchTargetIDs;

  public GraphQlOperationDispatch(
      VajramGraph vajramGraph,
      TypeDefinitionRegistry typeDefinitionRegistry,
      Set<Class<? extends GraphQlOperationAggregate_Req>> dispatchTargets) {
    this.graph = vajramGraph;
    this.typeDefinitionRegistry = typeDefinitionRegistry;
    Set<Class<? extends Request<?>>> reqs = new HashSet<>();
    for (Class<? extends GraphQlOperationAggregate_Req> dispatchTarget : dispatchTargets) {
      //noinspection unchecked
      reqs.add((Class<? extends Request<?>>) dispatchTarget);
    }
    //noinspection unchecked
    this.dispatchTargets = ImmutableSet.copyOf(reqs);
    this.dispatchTargetIDs =
        dispatchTargets.stream()
            .map(vajramGraph::getVajramIdByVajramReqType)
            .collect(toImmutableSet());
  }

  @Override
  public VajramID traitID() {
    return graph.getVajramIdByVajramReqType(GraphQlOperationAggregate_Req.class);
  }

  @Override
  public ImmutableSet<Class<? extends Request<?>>> dispatchTargetReqs() {
    return dispatchTargets;
  }

  @Override
  public ImmutableSet<VajramID> dispatchTargetIDs() {
    return dispatchTargetIDs;
  }

  @Override
  public @Nullable VajramID getDispatchTargetID(
      @Nullable Dependency dependency, Request<?> request) {
    if (request instanceof GraphQlOperationAggregate_Req<?> operationAggregateReq) {
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
      String resolvedOperationTypeName;
      Operation operationType = operationAggregateReq.operationType();
      if (operationType == null) {
        log.error(
            "Operation type is null - cannot compute dispatch target for {}. Forwarding as is.",
            operationAggregateReq);
        return null;
      }
      String operationName =
          switch (operationType) {
            case QUERY -> "query";
            case MUTATION -> "mutation";
            case SUBSCRIPTION -> "subscription";
          };
      OperationTypeDefinition operationDefinition = operationTypesByOpName.get(operationName);
      if (operationDefinition == null) {
        log.error(
            "{} operation has not been configured in schema {} - cannot compute dispatch target for {}. Forwarding as is.",
            operationName,
            schemaDefinition,
            operationAggregateReq);
        return null;
      } else {
        resolvedOperationTypeName = operationDefinition.getTypeName().getName();
      }
      return vajramID(resolvedOperationTypeName + GRAPHQL_AGGREGATOR_SUFFIX);
    }
    return request._vajramID();
  }
}
