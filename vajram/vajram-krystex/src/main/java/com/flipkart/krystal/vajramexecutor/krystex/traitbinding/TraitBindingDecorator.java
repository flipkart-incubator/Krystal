package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.vajram.exec.VajramDefinition;
import com.flipkart.krystal.vajram.traitbinding.TraitBindingProvider;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class TraitBindingDecorator implements DependencyDecorator {

  private final VajramKryonGraph vajramKryonGraph;
  private final TraitBindingProvider traitBindingProvider;

  public TraitBindingDecorator(
      VajramKryonGraph vajramKryonGraph, TraitBindingProvider traitBindingProvider) {
    this.vajramKryonGraph = vajramKryonGraph;
    this.traitBindingProvider = traitBindingProvider;
  }

  @Override
  public CompletableFuture<KryonResponse> invokeDependency(
      VajramID clientVajramID,
      Dependency dependency,
      Function<KryonCommand, CompletableFuture<KryonResponse>> invocationToDecorate,
      ForwardSend invocationRequest) {
    VajramID vajramID = invocationRequest.vajramID();
    Optional<VajramDefinition> vajramDefinition = vajramKryonGraph.getVajramDefinition(vajramID);
    return null; // TODO
  }
}
