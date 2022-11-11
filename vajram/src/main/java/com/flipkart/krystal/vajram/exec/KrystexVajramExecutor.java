package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.krystex.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.NodeDefinition;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.exec.VajramDAG.ResolverDefinition;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class KrystexVajramExecutor implements VajramExecutor {
  private final VajramGraph vajramGraph;
  private final KrystalNodeExecutor krystalExecutor;

  public KrystexVajramExecutor(VajramGraph vajramGraph, String requestId) {
    this.vajramGraph = vajramGraph;
    this.krystalExecutor =
        new KrystalNodeExecutor(vajramGraph.getNodeDefinitionRegistry(), requestId);
  }

  @Override
  public <T> CompletableFuture<T> requestExecution(String vajramId, VajramRequest request) {
    VajramDAG<T> vajramDAG = vajramGraph.createVajramDAG(vajramId);
    ImmutableList<ResolverDefinition> resolverDefinitions = vajramDAG.resolverDefinitions();
    ImmutableMap<String, Optional<Object>> inputs = request.asMap();
    for (ResolverDefinition resolverDefinition : resolverDefinitions) {
      NodeDefinition<?> nodeDefinition = resolverDefinition.nodeDefinition();
      Map<String, Optional<Object>> filteredInputs =
          Maps.filterKeys(inputs, input -> resolverDefinition.boundFrom().contains(input));
      krystalExecutor.executeWithInputs(nodeDefinition, filteredInputs);
    }

    return krystalExecutor
        .executeWithInputs(vajramDAG.vajramLogicNodeDefinition(), inputs)
        .getAllResults()
        .thenApply(
            results -> {
              if (results.size() != 1) {
                // This should never happen
                throw new AssertionError("Received incorrect number of results.");
              }
              return results.iterator().next();
            });
  }

  @Override
  public void close() {
    krystalExecutor.close();
  }
}
