package com.flipkart.krystal.krystex;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class NonBlockingNodeDefinition<T> extends NodeDefinition<T> {

  private final Function<NodeInputs, ImmutableList<T>> nonBlockingNodeLogic;

  @MonotonicNonNull private NodeLogic<T> nodeLogic;

  NonBlockingNodeDefinition(
      String nodeId,
      Set<String> inputNames,
      Map<String, String> inputProviders,
      Function<NodeInputs, ImmutableList<T>> nodeLogic) {
    super(nodeId, inputNames, inputProviders, ImmutableMap.of());
    nonBlockingNodeLogic = nodeLogic;
  }

  @Override
  public NodeLogic<T> logic() {
    if (nodeLogic == null) {
      this.nodeLogic =
          nodeInputs -> {
            Map<NodeInputs, MultiResult<T>> adaptedResult = new LinkedHashMap<>();
            for (NodeInputs nodeInput : nodeInputs) {
              adaptedResult.put(
                  nodeInput,
                  new MultiResult<>(completedFuture(nonBlockingNodeLogic.apply(nodeInput))));
            }
            return ImmutableMap.copyOf(adaptedResult);
          };
    }
    return nodeLogic;
  }
}
