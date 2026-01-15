package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatchingDecorator;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

public record KrystexVajramExecutorConfig(
    String requestId,
    @NonNull @CalledMethods("executorService") KryonExecutorConfig kryonExecutorConfig,
    @NonNull KrystexGraph graph) {

  @Builder(toBuilder = true)
  public KrystexVajramExecutorConfig {
    kryonExecutorConfig = primeConfig(kryonExecutorConfig, graph);
  }

  private static KryonExecutorConfig primeConfig(
      KryonExecutorConfig kryonExecutorConfig, KrystexGraph graph) {
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder = kryonExecutorConfig.toBuilder();
    kryonExecutorConfigBuilder.traitDispatchDecorator(graph.traitDispatchDecorator());
    if (!kryonExecutorConfig
        .outputLogicDecoratorConfigs()
        .containsKey(InputBatchingDecorator.DECORATOR_TYPE)) {
      kryonExecutorConfigBuilder.configureWith(graph.inputBatchingConfig());
    }
    if (!kryonExecutorConfig
        .kryonDecoratorConfigs()
        .containsKey(KryonInputInjector.DECORATOR_TYPE)) {
      kryonExecutorConfigBuilder.configureWith(graph.inputInjectionConfig());
    }
    return kryonExecutorConfigBuilder.build();
  }

  public static class KrystexVajramExecutorConfigBuilder {

    public @Nullable KryonExecutorConfig kryonExecutorConfig() {
      return kryonExecutorConfig;
    }

    public KrystexVajramExecutorConfigBuilder kryonExecutorConfig(
        KryonExecutorConfig kryonExecutorConfig) {
      this.kryonExecutorConfig = kryonExecutorConfig;
      return this;
    }
  }
}
