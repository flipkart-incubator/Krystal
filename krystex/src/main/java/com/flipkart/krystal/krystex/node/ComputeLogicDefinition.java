package com.flipkart.krystal.krystex.node;

import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.krystex.MultiResultFuture;
import com.flipkart.krystal.krystex.node.NodeInputs;
import com.flipkart.krystal.krystex.node.NodeLogic;
import com.flipkart.krystal.krystex.node.NodeLogicDefinition;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class ComputeLogicDefinition<T> extends NodeLogicDefinition<T> {

  private final Function<NodeInputs, ImmutableList<T>> computeLogic;

  @MonotonicNonNull private NodeLogic<T> nodeLogic;

  public ComputeLogicDefinition(
      NodeLogicId nodeLogicId,
      Set<String> inputNames,
      Function<NodeInputs, ImmutableList<T>> nodeLogic) {
    super(nodeLogicId, inputNames);
    computeLogic = nodeLogic;
  }

  @Override
  public NodeLogic<T> logic() {
    if (nodeLogic == null) {
      this.nodeLogic =
          nodeInputs -> {
            Map<NodeInputs, MultiResultFuture<T>> adaptedResult = new LinkedHashMap<>();
            for (NodeInputs nodeInput : nodeInputs) {
              adaptedResult.put(
                  nodeInput,
                  new MultiResultFuture<>(completedFuture(computeLogic.apply(nodeInput))));
            }
            return ImmutableMap.copyOf(adaptedResult);
          };
    }
    return nodeLogic;
  }
}
