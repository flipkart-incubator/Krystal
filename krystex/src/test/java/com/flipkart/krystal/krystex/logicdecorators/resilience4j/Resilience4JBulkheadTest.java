package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.GRANULAR;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.ForkJoinExecutorPool;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonId;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.logicdecoration.MainLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Resilience4JBulkheadTest {

  private static final Duration TIMEOUT = ofSeconds(1);
  private KryonExecutor kryonExecutor;
  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;

  @BeforeEach
  void setUp() {
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
    this.kryonExecutor =
        new KryonExecutor(
            kryonDefinitionRegistry,
            new ForkJoinExecutorPool(1),
            KryonExecutorConfig.builder().kryonExecStrategy(GRANULAR).build(),
            "test");
  }

  @Test
  void bulkhead_restrictsConcurrency() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    MainLogicDefinition<String> mainLogic =
        newAsyncLogic(
            "bulkhead_restrictsConcurrency",
            Set.of("input"),
            dependencyValues ->
                supplyAsync(
                    () -> {
                      while (countDownLatch.getCount() > 0) {
                        try {
                          countDownLatch.await();
                        } catch (InterruptedException ignored) {
                        }
                      }
                      return "computed_value";
                    },
                    executorService));
    Resilience4JBulkhead resilience4JBulkhead =
        new Resilience4JBulkhead("bulkhead_restrictsConcurrency");
    resilience4JBulkhead.onConfigUpdate(
        new ConfigProvider() {
          @SuppressWarnings("unchecked")
          @Override
          public <T> Optional<T> getConfig(String key) {
            return switch (key) {
              case "bulkhead_restrictsConcurrency.bulkhead.max_concurrency" -> (Optional<T>)
                  Optional.of(2);
              case "bulkhead_restrictsConcurrency.bulkhead.enabled" -> (Optional<T>)
                  Optional.of(true);
              default -> Optional.empty();
            };
          }
        });
    mainLogic.registerRequestScopedDecorator(
        List.of(
            new MainLogicDecoratorConfig(
                Resilience4JBulkhead.DECORATOR_TYPE,
                (logicExecutionContext) -> true,
                logicExecutionContext -> "",
                decoratorContext -> resilience4JBulkhead)));
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition("kryon", mainLogic.kryonLogicId());

    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(1))),
            KryonExecutionConfig.builder().executionId("req_1").build());
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(2))),
            KryonExecutionConfig.builder().executionId("req_2").build());
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(3))),
            KryonExecutionConfig.builder().executionId("req_3").build());
    kryonExecutor.flush();
    assertThat(callAfterBulkheadExhaustion)
        .failsWithin(TIMEOUT)
        .withThrowableOfType(Exception.class)
        .withRootCauseInstanceOf(BulkheadFullException.class)
        .withMessageContaining(
            "Bulkhead 'bulkhead_restrictsConcurrency.bulkhead' is full and does not permit further calls");
    countDownLatch.countDown();
    assertThat(call1BeforeBulkheadExhaustion).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
    assertThat(call2BeforeBulkheadExhaustion).succeedsWithin(TIMEOUT).isEqualTo("computed_value");
  }

  @Test
  void threadpoolBulkhead_restrictsConcurrency() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    MainLogicDefinition<String> mainLogic =
        newAsyncLogic(
            "threadpoolBulkhead_restrictsConcurrency",
            Set.of("input"),
            dependencyValues -> {
              while (countDownLatch.getCount() > 0) {
                try {
                  countDownLatch.await();
                } catch (InterruptedException ignored) {
                }
              }
              return completedFuture("computed_value");
            });
    Resilience4JBulkhead resilience4JBulkhead =
        new Resilience4JBulkhead("threadpoolBulkhead_restrictsConcurrency");
    resilience4JBulkhead.onConfigUpdate(
        new ConfigProvider() {
          @SuppressWarnings("unchecked")
          @Override
          public <T> Optional<T> getConfig(String key) {
            return switch (key) {
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.max_concurrency" -> (Optional<
                      T>)
                  Optional.of(2);
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.enabled" -> (Optional<T>)
                  Optional.of(true);
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.type" -> (Optional<T>)
                  Optional.of("THREADPOOL");
              default -> Optional.empty();
            };
          }
        });
    mainLogic.registerRequestScopedDecorator(
        List.of(
            new MainLogicDecoratorConfig(
                Resilience4JBulkhead.DECORATOR_TYPE,
                (logicExecutionContext) -> true,
                logicExecutionContext -> "",
                decoratorContext -> resilience4JBulkhead)));
    KryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newKryonDefinition("kryon", mainLogic.kryonLogicId());

    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(1))),
            KryonExecutionConfig.builder().executionId("req_1").build());
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(2))),
            KryonExecutionConfig.builder().executionId("req_2").build());
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        kryonExecutor.executeKryon(
            kryonDefinition.kryonId(),
            new Inputs(ImmutableMap.of("input", withValue(3))),
            KryonExecutionConfig.builder().executionId("req_3").build());
    kryonExecutor.flush();
    assertThat(callAfterBulkheadExhaustion)
        .failsWithin(1, HOURS)
        .withThrowableOfType(Exception.class)
        .withMessageContaining(
            "Bulkhead 'threadpoolBulkhead_restrictsConcurrency.bulkhead' is full and does not permit further calls");
    countDownLatch.countDown();
    assertThat(call1BeforeBulkheadExhaustion)
        .succeedsWithin(1, SECONDS)
        .isEqualTo("computed_value");
    assertThat(call2BeforeBulkheadExhaustion)
        .succeedsWithin(1, SECONDS)
        .isEqualTo("computed_value");
  }

  private <T> MainLogicDefinition<T> newAsyncLogic(
      String kryonId, Set<String> inputs, Function<Inputs, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<>(
            new KryonLogicId(new KryonId(kryonId), kryonId + ":asyncLogic"),
            inputs,
            inputsList ->
                inputsList.stream()
                    .collect(ImmutableMap.toImmutableMap(Function.identity(), logic)),
            ImmutableMap.of());
    logicDefinitionRegistry.addMainLogic(def);
    return def;
  }
}
