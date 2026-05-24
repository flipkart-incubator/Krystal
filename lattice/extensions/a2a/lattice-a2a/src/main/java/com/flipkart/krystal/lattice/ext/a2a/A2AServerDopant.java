package com.flipkart.krystal.lattice.ext.a2a;

import static java.util.concurrent.CompletableFuture.failedFuture;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.lattice.core.doping.Dopant;
import com.flipkart.krystal.lattice.core.doping.DopantConfig.NoConfiguration;
import com.flipkart.krystal.lattice.core.doping.DopantType;
import com.flipkart.krystal.lattice.ext.a2a.api.A2AServer;
import com.flipkart.krystal.lattice.krystex.KrystexDopant;
import com.flipkart.krystal.lattice.vajram.VajramRequestExecutionContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.CompletionStage;

/**
 * Dopant that bridges A2A agent skill invocations to the Krystal Vajram execution engine.
 *
 * <p>The codegen (see {@code lattice-a2a-codegen}) generates an {@code AgentExecutor}
 * implementation that calls {@link #executeAgent} and {@link #cancelAgent} with the appropriate
 * {@link ImmutableRequest} built from the incoming A2A {@code RequestContext}.
 */
@Singleton
@DopantType(A2AServerDopant.A2A_SERVER_DOPANT_TYPE)
public final class A2AServerDopant implements Dopant<A2AServer, NoConfiguration> {

  static final String A2A_SERVER_DOPANT_TYPE = "krystal.lattice.a2aServer";

  private final KrystexDopant krystexDopant;

  @Inject
  public A2AServerDopant(KrystexDopant krystexDopant) {
    this.krystexDopant = krystexDopant;
  }

  /** Executes the executor Vajram for an incoming A2A task. */
  public <T> CompletionStage<T> executeAgent(ImmutableRequest<T> vajramRequest) {
    return executeVajram(vajramRequest);
  }

  /** Executes the canceller Vajram for an incoming A2A cancel request. */
  public <T> CompletionStage<T> cancelAgent(ImmutableRequest<T> vajramRequest) {
    return executeVajram(vajramRequest);
  }

  private <T> CompletionStage<T> executeVajram(ImmutableRequest<T> vajramRequest) {
    try {
      return krystexDopant.executeRequest(
          VajramRequestExecutionContext.<T>builder().vajramRequest(vajramRequest).build());
    } catch (Exception e) {
      return failedFuture(e);
    }
  }
}
