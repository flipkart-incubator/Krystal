package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.flipkart.krystal.krystex.logicdecorators.resilience4j.R4JUtils.extractResponseMap;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig.Builder;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.decorators.Decorators;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Resilience4JBulkhead implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JBulkhead.class.getName();

  private enum BulkheadType {
    THREADPOOL,
    SEMAPHORE
  }

  private final String instanceId;

  private @Nullable BulkheadAdapter adaptedBulkhead;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   */
  public Resilience4JBulkhead(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    BulkheadAdapter bulkhead = this.adaptedBulkhead;
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
    Optional<BulkheadAdapterConfig> newBulkheadConfig = getBulkheadConfig(configProvider);
    if (newBulkheadConfig.isPresent()) {
      BulkheadAdapter bulkhead = this.adaptedBulkhead;
      if (bulkhead == null) {
        this.adaptedBulkhead = new BulkheadAdapter(newBulkheadConfig.get());
      } else {
        bulkhead.changeConfig(newBulkheadConfig.get());
      }
    } else {
      this.adaptedBulkhead = null;
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
    private @Nullable Bulkhead bulkhead;
    private @Nullable ThreadPoolBulkhead threadPoolBulkhead;

    private BulkheadAdapter(BulkheadAdapterConfig config) {
      BulkheadConfig bulkheadConfig = config.bulkheadConfig();
      if (bulkheadConfig != null) {
        this.bulkhead = Bulkhead.of(getBulkheadId(), bulkheadConfig);
      } else {
        ThreadPoolBulkheadConfig threadPoolBulkheadConfig = config.threadPoolBulkheadConfig();
        if (threadPoolBulkheadConfig != null) {
          this.threadPoolBulkhead =
              newThreadPoolBulkhead(threadPoolBulkheadConfig, getBulkheadId());
        } else {
          throw new IllegalArgumentException(
              "Either bulkheadConfig or threadPoolBulkheadConfig must be non-null");
        }
      }
    }

    private void changeConfig(BulkheadAdapterConfig config) {
      Bulkhead localBulkHead = bulkhead;
      if ((config.bulkheadConfig() != null)
          && localBulkHead != null
          && !config.bulkheadConfig().equals(localBulkHead.getBulkheadConfig())) {
        localBulkHead.changeConfig(config.bulkheadConfig());
      } else {
        ThreadPoolBulkhead localTPBulkhead = threadPoolBulkhead;
        if (config.threadPoolBulkheadConfig() != null
            && localTPBulkhead != null
            && !config.threadPoolBulkheadConfig().equals(localTPBulkhead.getBulkheadConfig())) {
          threadPoolBulkhead =
              newThreadPoolBulkhead(config.threadPoolBulkheadConfig(), getBulkheadId());
        }
      }
    }

    @SuppressWarnings("RedundantTypeArguments") // Avoid nullChecker errors
    CompletionStage<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>> decorate(
        OutputLogic<Object> logicToDecorate, ImmutableList<Inputs> inputsList) {
      ThreadPoolBulkhead threadPoolBulkhead = this.threadPoolBulkhead;
      Bulkhead bulkhead = this.bulkhead;
      if (threadPoolBulkhead != null) {
        return threadPoolBulkhead
            .<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>executeCallable(
                () -> logicToDecorate.execute(inputsList));
      } else if (bulkhead != null) {
        return Decorators
            .<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>ofCompletionStage(
                () -> {
                  ImmutableMap<Inputs, CompletableFuture<@Nullable Object>> result =
                      logicToDecorate.execute(inputsList);
                  return allOf(result.values().toArray(CompletableFuture[]::new))
                      .<ImmutableMap<Inputs, CompletableFuture<@Nullable Object>>>handle(
                          (unused, throwable) -> result);
                })
            .withBulkhead(bulkhead)
            .get();
      } else {
        throw new IllegalStateException(
            "Either bulkheadConfig or threadPoolBulkheadConfig must be non-null");
      }
    }

    private static ThreadPoolBulkhead newThreadPoolBulkhead(
        ThreadPoolBulkheadConfig config, String bulkheadId) {
      return ThreadPoolBulkhead.of(bulkheadId, config);
    }
  }

  private String getBulkheadId() {
    return instanceId + ".bulkhead";
  }

  private record BulkheadAdapterConfig(
      @Nullable BulkheadConfig bulkheadConfig,
      @Nullable ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
    BulkheadAdapterConfig {
      checkArgument(bulkheadConfig == null || threadPoolBulkheadConfig == null);
      checkArgument(bulkheadConfig != null || threadPoolBulkheadConfig != null);
    }

    private BulkheadAdapterConfig(BulkheadConfig bulkheadConfig) {
      this(bulkheadConfig, null);
    }

    private BulkheadAdapterConfig(ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
      this(null, threadPoolBulkheadConfig);
    }
  }

  @Override
  public void onComplete() {
    // do nothing
  }
}
