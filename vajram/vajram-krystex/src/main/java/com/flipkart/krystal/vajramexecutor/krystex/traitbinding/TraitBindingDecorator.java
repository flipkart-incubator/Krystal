package com.flipkart.krystal.vajramexecutor.krystex.traitbinding;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ClientSideCommand;
import com.flipkart.krystal.krystex.commands.Flush;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.dependencydecoration.DependencyDecorator;
import com.flipkart.krystal.krystex.dependencydecoration.VajramInvocation;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import com.flipkart.krystal.traits.TraitBindingProvider;
import java.util.concurrent.CompletableFuture;

public class TraitBindingDecorator implements DependencyDecorator {

  public static final String DECORATOR_TYPE = TraitBindingProvider.class.getName();

  private final TraitBindingProvider traitBindingProvider;

  public TraitBindingDecorator(TraitBindingProvider traitBindingProvider) {
    this.traitBindingProvider = traitBindingProvider;
  }

  @Override
  public <R extends KryonResponse> CompletableFuture<R> invokeDependency(
      ClientSideCommand<R> kryonCommand, VajramInvocation<R> invocationToDecorate) {
    Dependency dependency = kryonCommand.dependantChain().latestDependency();
    if (dependency != null) {
      VajramID traitId = kryonCommand.vajramID();
      VajramID boundVajram = traitBindingProvider.get(traitId, dependency);
      if (kryonCommand instanceof ForwardSend forwardSend) {
        return invocationToDecorate.invokeDependency(
            (ClientSideCommand<R>)
                new ForwardSend(
                    boundVajram,
                    forwardSend.executableRequests(),
                    forwardSend.dependantChain(),
                    forwardSend.skippedRequests()));
      } else if (kryonCommand instanceof Flush) {
        return invocationToDecorate.invokeDependency(
            (ClientSideCommand<R>) new Flush(boundVajram, kryonCommand.dependantChain()));
      }
    }
    return invocationToDecorate.invokeDependency(kryonCommand);
  }
}
