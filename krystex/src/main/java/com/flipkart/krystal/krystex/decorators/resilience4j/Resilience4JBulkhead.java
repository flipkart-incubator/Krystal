package com.flipkart.krystal.krystex.decorators.resilience4j;

import static com.flipkart.krystal.krystex.decorators.resilience4j.R4JUtils.extractResponseMap;
import static io.github.resilience4j.decorators.Decorators.ofCompletionStage;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig.Builder;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public final class Resilience4JBulkhead implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JBulkhead.class.getName();

  private enum BulkheadType {
    THREADPOOL,
    SEMAPHORE
  }

  private final String instanceId;

  private BulkheadAdapter bulkhead;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   */
  public Resilience4JBulkhead(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public MainLogic<Object> decorateLogic(
      MainLogic<Object> logicToDecorate, MainLogicDefinition<Object> originalLogicDefinition) {
    BulkheadAdapter bulkhead = this.bulkhead;
    if (bulkhead != null) {
      return inputsList ->
          extractResponseMap(inputsList, bulkhead.decorate(logicToDecorate, inputsList));
    } else {
      return logicToDecorate;
    }
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    updateBulkhead(configProvider);
  }

  @Override
  public String getId() {
    return instanceId;
  }

  private void updateBulkhead(ConfigProvider configProvider) {
    BulkheadAdapter bulkhead = this.bulkhead;
    Optional<BulkheadAdapterConfig> newBulkheadConfig = getBulkheadConfig(configProvider);
    if (newBulkheadConfig.isPresent()) {
      if (bulkhead == null) {
        this.bulkhead = new BulkheadAdapter(newBulkheadConfig.get());
      } else {
        bulkhead.changeConfig(newBulkheadConfig.get());
      }
    }
  }

  private Optional<BulkheadAdapterConfig> getBulkheadConfig(ConfigProvider configProvider) {
    boolean bulkheadEnabled =
        configProvider.<Boolean>getConfig(instanceId + ".bulkhead.enabled").orElse(true);
    if (!bulkheadEnabled) {
      return Optional.empty();
    }
    BulkheadType bulkheadType =
        configProvider
            .<String>getConfig(instanceId + ".bulkhead.type")
            .map(BulkheadType::valueOf)
            .orElse(BulkheadType.SEMAPHORE);
    Optional<Integer> maxConcurrency =
        configProvider.getConfig(instanceId + ".bulkhead.max_concurrency");
    switch (bulkheadType) {
      case SEMAPHORE -> {
        Builder builder = BulkheadConfig.custom().writableStackTraceEnabled(false);
        maxConcurrency.ifPresent(builder::maxConcurrentCalls);
        return Optional.of(new BulkheadAdapterConfig(builder.build()));
      }
      case THREADPOOL -> {
        ThreadPoolBulkheadConfig.Builder builder =
            ThreadPoolBulkheadConfig.custom()
                .writableStackTraceEnabled(false)
                // This means requests received after bulkhead exhaustion will instantly fail.
                // Setting this to anything other than 0 means that many pending requests will wait
                // even if bulkhead is full.
                .queueCapacity(0);
        maxConcurrency.ifPresent(builder::maxThreadPoolSize);
        maxConcurrency.ifPresent(builder::coreThreadPoolSize);
        return Optional.of(new BulkheadAdapterConfig(builder.build()));
      }
      default -> {
        return Optional.empty();
      }
    }
  }

  private final class BulkheadAdapter {
    private Bulkhead bulkhead;
    private ThreadPoolBulkhead threadPoolBulkhead;

    private BulkheadAdapter(BulkheadAdapterConfig config) {
      if (config.bulkheadConfig() != null) {
        this.bulkhead = Bulkhead.of(getBulkheadId(), config.bulkheadConfig());
      } else if (config.threadPoolBulkheadConfig() != null) {
        this.threadPoolBulkhead = newThreadPoolBulkhead(config.threadPoolBulkheadConfig());
      }
    }

    private void changeConfig(BulkheadAdapterConfig config) {
      if ((config.bulkheadConfig() != null)
          && bulkhead != null
          && !config.bulkheadConfig().equals(bulkhead.getBulkheadConfig())) {
        bulkhead.changeConfig(config.bulkheadConfig());
      } else if (config.threadPoolBulkheadConfig() != null
          && threadPoolBulkhead != null
          && !config.threadPoolBulkheadConfig().equals(threadPoolBulkhead.getBulkheadConfig())) {
        threadPoolBulkhead = newThreadPoolBulkhead(config.threadPoolBulkheadConfig());
      } else {
        throw new IllegalStateException();
      }
    }

    CompletionStage<ImmutableMap<Inputs, CompletableFuture<Object>>> decorate(
        MainLogic<Object> logicToDecorate, ImmutableList<Inputs> inputsList) {
      if (bulkhead != null) {
        return ofCompletionStage(
                () -> {
                  ImmutableMap<Inputs, CompletableFuture<Object>> result =
                      logicToDecorate.execute(inputsList);
                  return allOf(result.values().toArray(CompletableFuture[]::new))
                      .handle((unused, throwable) -> result);
                })
            .withBulkhead(bulkhead)
            .get();
      } else if (threadPoolBulkhead != null) {
        return threadPoolBulkhead.executeCallable(() -> logicToDecorate.execute(inputsList));
      }
      return null;
    }

    private ThreadPoolBulkhead newThreadPoolBulkhead(ThreadPoolBulkheadConfig config) {
      return ThreadPoolBulkhead.of(getBulkheadId(), config);
    }

    private String getBulkheadId() {
      return instanceId + ".bulkhead";
    }
  }

  private record BulkheadAdapterConfig(
      BulkheadConfig bulkheadConfig, ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
    BulkheadAdapterConfig {
      assert bulkheadConfig == null || threadPoolBulkheadConfig == null;
      assert bulkheadConfig != null || threadPoolBulkheadConfig != null;
    }

    private BulkheadAdapterConfig(BulkheadConfig bulkheadConfig) {
      this(bulkheadConfig, null);
    }

    private BulkheadAdapterConfig(ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
      this(null, threadPoolBulkheadConfig);
    }
  }
}
