package com.flipkart.krystal.vajramexecutor.krystex;

import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.kryondecoration.KryonExecutionContext;
import com.flipkart.krystal.vajram.inputinjection.VajramInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatchingDecorator;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.checker.nullness.qual.Nullable;

public record KrystexVajramExecutorConfig(
    String requestId,
    @NonNull @CalledMethods("singleThreadExecutor") KryonExecutorConfig kryonExecutorConfig,
    @NonNull VajramKryonGraph vajramKryonGraph,
    @Nullable VajramInjectionProvider inputInjectionProvider) {

  @Builder(toBuilder = true)
  public KrystexVajramExecutorConfig {
    kryonExecutorConfig =
        primeConfig(kryonExecutorConfig, inputInjectionProvider, vajramKryonGraph);
  }

  public KryonExecutorConfigBuilder kryonExecutorConfigBuilder() {
    return kryonExecutorConfig.toBuilder();
  }

  private static KryonExecutorConfig primeConfig(
      KryonExecutorConfig kryonExecutorConfig,
      @Nullable VajramInjectionProvider inputInjectionProvider,
      VajramKryonGraph graph) {
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
      kryonExecutorConfigBuilder.configureWith(kryonInputInjector(graph, inputInjectionProvider));
    }
    return kryonExecutorConfigBuilder.build();
  }

  private static KryonExecutorConfigurator kryonInputInjector(
      VajramKryonGraph vajramKryonGraph, @Nullable VajramInjectionProvider injectionProvider) {
    if (injectionProvider == null) {
      return KryonExecutorConfigurator.NO_OP;
    }
    return configBuilder ->
        configBuilder.kryonDecoratorConfig(
            KryonInputInjector.DECORATOR_TYPE,
            new KryonDecoratorConfig(
                KryonInputInjector.DECORATOR_TYPE,
                /* shouldDecorate= */ executorContext ->
                    isInjectionNeeded(vajramKryonGraph, executorContext),
                /* instanceIdGenerator= */ executionContext -> KryonInputInjector.DECORATOR_TYPE,
                /* factory= */ decoratorContext ->
                    new KryonInputInjector(vajramKryonGraph, injectionProvider)));
  }

  private static boolean isInjectionNeeded(
      VajramKryonGraph vajramKryonGraph, KryonExecutionContext executionContext) {
    return vajramKryonGraph
        .getVajramDefinition(executionContext.vajramID())
        .metadata()
        .isInputInjectionNeeded();
  }
}
