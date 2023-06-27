package com.flipkart.krystal.krystex.decorators.resilience4j;

import static com.flipkart.krystal.data.ValueOrError.withValue;
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
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.node.KrystalNodeExecutor;
import com.flipkart.krystal.krystex.node.NodeDefinition;
import com.flipkart.krystal.krystex.node.NodeDefinitionRegistry;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.NodeLogicId;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Resilience4JBulkheadTest {
  private KrystalNodeExecutor krystalNodeExecutor;
  private NodeDefinitionRegistry nodeDefinitionRegistry;
  private LogicDefinitionRegistry logicDefinitionRegistry;

  @BeforeEach
  void setUp() {
    this.logicDefinitionRegistry = new LogicDefinitionRegistry();
    this.nodeDefinitionRegistry = new NodeDefinitionRegistry(logicDefinitionRegistry);
    this.krystalNodeExecutor =
        new KrystalNodeExecutor(
            nodeDefinitionRegistry,
            new LogicDecorationOrdering(ImmutableSet.of()),
            new ForkJoinExecutorPool(1),
            "test",
            ImmutableMap.of());
  }

  @Test
  void bulkhead_restrictsConcurrency() {
    CountDownLatch countDownLatch = new CountDownLatch(1);
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
                    Executors.newSingleThreadExecutor()));
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
    NodeDefinition nodeDefinition =
        nodeDefinitionRegistry.newNodeDefinition("node", mainLogic.nodeLogicId());

    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(1))), "req_1");
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(2))), "req_2");
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(3))), "req_3");
    krystalNodeExecutor.flush();
    assertThat(callAfterBulkheadExhaustion)
        .failsWithin(1, SECONDS)
        .withThrowableOfType(Exception.class)
        .withRootCauseInstanceOf(BulkheadFullException.class)
        .withMessageContaining(
            "Bulkhead 'bulkhead_restrictsConcurrency.bulkhead' is full and does not permit further calls");
    countDownLatch.countDown();
    assertThat(call1BeforeBulkheadExhaustion)
        .succeedsWithin(1, SECONDS)
        .isEqualTo("computed_value");
    assertThat(call2BeforeBulkheadExhaustion)
        .succeedsWithin(1, SECONDS)
        .isEqualTo("computed_value");
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
    NodeDefinition nodeDefinition =
        nodeDefinitionRegistry.newNodeDefinition("node", mainLogic.nodeLogicId());

    CompletableFuture<Object> call1BeforeBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(1))), "req_1");
    CompletableFuture<Object> call2BeforeBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(2))), "req_2");
    CompletableFuture<Object> callAfterBulkheadExhaustion =
        krystalNodeExecutor.executeNode(
            nodeDefinition.nodeId(), new Inputs(ImmutableMap.of("input", withValue(3))), "req_3");
    krystalNodeExecutor.flush();
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
      String nodeId, Set<String> inputs, Function<Inputs, CompletableFuture<T>> logic) {
    IOLogicDefinition<T> def =
        new IOLogicDefinition<>(
            new NodeLogicId(new NodeId(nodeId), nodeId + ":asyncLogic"),
            inputs,
            inputsList ->
                inputsList.stream()
                    .collect(ImmutableMap.toImmutableMap(Function.identity(), logic)),
            ImmutableMap.of());
    logicDefinitionRegistry.addMainLogic(def);
    return def;
  }
}
