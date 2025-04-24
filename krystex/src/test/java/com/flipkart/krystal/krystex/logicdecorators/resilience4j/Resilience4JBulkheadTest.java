package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.krystex.testutils.SimpleFacet.input;
import static com.flipkart.krystal.tags.ElementTags.emptyTags;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.annos.ExternallyInvocable;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.krystex.testutils.SimpleRequestBuilder;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.InputMirror;
import com.flipkart.krystal.krystex.IOLogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinition;
import com.flipkart.krystal.krystex.LogicDefinitionRegistry;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.KryonDefinitionRegistry;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonLogicId;
import com.flipkart.krystal.krystex.kryon.VajramKryonDefinition;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.resolution.CreateNewRequest;
import com.flipkart.krystal.krystex.resolution.FacetsFromRequest;
import com.flipkart.krystal.krystex.testutils.FacetValuesMapBuilder;
import com.flipkart.krystal.krystex.testutils.SimpleFacet;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Resilience4JBulkheadTest {

  private static final Duration TIMEOUT = ofSeconds(1);
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private Lease<SingleThreadExecutor> executorLease;
  private KryonDefinitionRegistry kryonDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.kryonDefinitionRegistry = new KryonDefinitionRegistry(logicDefinitionRegistry);
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void bulkhead_restrictsConcurrency() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    OutputLogicDefinition<String> outputLogicDef =
        newAsyncLogic(
            "bulkhead_restrictsConcurrency",
            ImmutableSet.of(input(1)),
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
              case "bulkhead_restrictsConcurrency.bulkhead.max_concurrency" ->
                  (Optional<T>) Optional.of(2);
              case "bulkhead_restrictsConcurrency.bulkhead.enabled" ->
                  (Optional<T>) Optional.of(true);
              default -> Optional.empty();
            };
          }
        });
    outputLogicDef.registerSessionScopedLogicDecorator(
        new OutputLogicDecoratorConfig(
            Resilience4JBulkhead.DECORATOR_TYPE,
            (logicExecutionContext) -> true,
            logicExecutionContext -> "",
            decoratorContext -> resilience4JBulkhead));
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            "kryon",
            Set.of(input(1)),
            outputLogicDef.kryonLogicId(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(Set.of(input(1))),
            newFacetsFromRequestLogic(),
            ElementTags.of(ExternallyInvocable.Creator.create()));

    KryonExecutor executor1 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor1");
    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        executor1.executeKryon(
            new SimpleRequestBuilder<>(
                    Set.of(input(1)), ImmutableMap.of(1, withValue(1)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    KryonExecutor executor2 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor2");
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        executor2.executeKryon(
            new SimpleRequestBuilder<>(
                    Set.of(input(1)), ImmutableMap.of(1, withValue(2)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_2").build());
    KryonExecutor executor3 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor3");
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        executor3.executeKryon(
            new SimpleRequestBuilder<>(
                    Set.of(input(1)), ImmutableMap.of(1, withValue(3)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_3").build());
    executor1.close();
    executor2.close();
    executor3.close();

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
    Set<SimpleFacet> inputs = Set.of(input(1));
    OutputLogicDefinition<String> outputLogic =
        newAsyncLogic(
            "threadpoolBulkhead_restrictsConcurrency",
            inputs,
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
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.max_concurrency" ->
                  (Optional<T>) Optional.of(2);
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.enabled" ->
                  (Optional<T>) Optional.of(true);
              case "threadpoolBulkhead_restrictsConcurrency.bulkhead.type" ->
                  (Optional<T>) Optional.of("THREADPOOL");
              default -> Optional.empty();
            };
          }
        });
    outputLogic.registerSessionScopedLogicDecorator(
        new OutputLogicDecoratorConfig(
            Resilience4JBulkhead.DECORATOR_TYPE,
            (logicExecutionContext) -> true,
            logicExecutionContext -> "",
            decoratorContext -> resilience4JBulkhead));
    VajramKryonDefinition kryonDefinition =
        kryonDefinitionRegistry.newVajramKryonDefinition(
            "kryon",
            inputs,
            outputLogic.kryonLogicId(),
            ImmutableMap.of(),
            ImmutableMap.of(),
            newCreateNewRequestLogic(inputs),
            newFacetsFromRequestLogic(),
            ElementTags.of(ExternallyInvocable.Creator.create()));

    KryonExecutor executor1 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor1");
    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        executor1.executeKryon(
            new SimpleRequestBuilder<>(
                    inputs, ImmutableMap.of(1, withValue(1)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_1").build());
    KryonExecutor executor2 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor2");
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        executor2.executeKryon(
            new SimpleRequestBuilder<>(
                    inputs, ImmutableMap.of(1, withValue(2)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_2").build());
    KryonExecutor executor3 =
        new KryonExecutor(
            kryonDefinitionRegistry,
            KryonExecutorConfig.builder().singleThreadExecutor(executorLease.get()).build(),
            "executor3");
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        executor3.executeKryon(
            new SimpleRequestBuilder<>(
                    inputs, ImmutableMap.of(1, withValue(3)), kryonDefinition.vajramID())
                ._build(),
            KryonExecutionConfig.builder().executionId("req_3").build());
    executor1.close();
    executor2.close();
    executor3.close();
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

  private <T> OutputLogicDefinition<T> newAsyncLogic(
      String kryonId,
      Set<? extends Facet> usedFacets,
      Function<FacetValues, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<T>(
            new KryonLogicId(new VajramID(kryonId), kryonId + ":asyncLogic"),
            usedFacets,
            inputsList ->
                inputsList.stream()
                    .collect(ImmutableMap.toImmutableMap(Function.identity(), logic)),
            emptyTags());
    logicDefinitionRegistry.addOutputLogic(def);
    return def;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  private static LogicDefinition<FacetsFromRequest> newFacetsFromRequestLogic() {
    VajramID vajramID = new VajramID("kryon");
    return new LogicDefinition<>(
        new KryonLogicId(vajramID, "kryon:facetsFromRequest"),
        request ->
            new FacetValuesMapBuilder(
                (SimpleRequestBuilder<Object>) request._asBuilder(), Set.of(), vajramID));
  }

  @NonNull
  private static LogicDefinition<CreateNewRequest> newCreateNewRequestLogic(
      Set<? extends InputMirror> inputs) {
    VajramID vajramID = new VajramID("kryon");
    return new LogicDefinition<>(
        new KryonLogicId(vajramID, "kryon:newRequest"),
        () -> new SimpleRequestBuilder<>(inputs, vajramID));
  }
}
