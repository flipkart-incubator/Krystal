package com.flipkart.krystal.krystex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public abstract non-sealed class IONodeDefinition<T> extends NodeDefinition<T> {

  private IoNodeAdaptor<T> ioNodeAdaptor;

  IONodeDefinition(
      String nodeId,
      Set<String> inputs,
      Map<String, String> inputProviders,
      ImmutableMap<String, String> groupMemberships) {
    super(nodeId, inputs, inputProviders, groupMemberships);
    this.ioNodeAdaptor = IoNodeAdaptor.noop();
  }

  public abstract ImmutableMap<NodeInputs, CompletableFuture<T>> modulatedLogic(
      ImmutableList<NodeInputs> modulatedRequests);

  @Override
  public final CompletableFuture<ImmutableList<T>> logic(NodeInputs dependencyValues) {
    return ioNodeAdaptor.adaptLogic(this::modulatedLogic).apply(dependencyValues);
  }

  public void setIoNodeAdaptor(IoNodeAdaptor<T> ioNodeAdaptor) {
    this.ioNodeAdaptor = ioNodeAdaptor;
  }
}
