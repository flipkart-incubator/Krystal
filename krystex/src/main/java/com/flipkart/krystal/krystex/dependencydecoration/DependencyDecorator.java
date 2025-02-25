package com.flipkart.krystal.krystex.dependencydecoration;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.krystex.commands.ForwardSend;
import com.flipkart.krystal.krystex.commands.KryonCommand;
import com.flipkart.krystal.krystex.kryon.KryonResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface DependencyDecorator {

  CompletableFuture<KryonResponse> invokeDependency(
      VajramID clientVajramID,
      Dependency dependency,
      Function<KryonCommand, CompletableFuture<KryonResponse>> invocationToDecorate,
      ForwardSend invocationRequest);
}
