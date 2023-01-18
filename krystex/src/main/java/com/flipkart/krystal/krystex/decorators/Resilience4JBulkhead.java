package com.flipkart.krystal.krystex.decorators;

import static io.github.resilience4j.decorators.Decorators.ofFunction;

import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.config.ConfigProvider;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig.Builder;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import java.util.Optional;

public final class Resilience4JBulkhead implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JBulkhead.class.getName();

  private final ConfigProvider configProvider;
  private final String instanceId;

  private Bulkhead bulkhead;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   * @param configProvider The configs for this logic decorator are read from this configProvider.
   */
  public Resilience4JBulkhead(String instanceId, ConfigProvider configProvider) {
    this.instanceId = instanceId;
    this.configProvider = configProvider;
    init();
  }

  @Override
  public MainLogic<Object> decorateLogic(MainLogic<Object> logicToDecorate) {
    Bulkhead bulkhead = this.bulkhead;
    if (bulkhead != null) {
      return ofFunction(logicToDecorate::execute).withBulkhead(bulkhead)::apply;
    } else {
      return logicToDecorate;
    }
  }

  @Override
  public void onConfigUpdate() {
    updateBulkhead();
  }

  @Override
  public String getId() {
    return instanceId;
  }

  private void init() {
    this.bulkhead =
        getBulkheadConfig()
            .map(bulkheadConfig -> new SemaphoreBulkhead(instanceId + ".bulkhead", bulkheadConfig))
            .orElse(null);
  }

  private void updateBulkhead() {
    Bulkhead bulkhead = this.bulkhead;
    Optional<BulkheadConfig> newBulkheadConfig = getBulkheadConfig();
    if (!Optional.ofNullable(bulkhead).map(Bulkhead::getBulkheadConfig).equals(newBulkheadConfig)) {
      if (bulkhead != null && newBulkheadConfig.isPresent()) {
        bulkhead.changeConfig(newBulkheadConfig.get());
      } else {
        init();
      }
    }
  }

  private Optional<BulkheadConfig> getBulkheadConfig() {
    boolean bulkheadEnabled =
        configProvider.<Boolean>getConfig(instanceId + ".bulkhead.enabled").orElse(true);
    if (!bulkheadEnabled) {
      return Optional.empty();
    }
    Builder builder = BulkheadConfig.custom().writableStackTraceEnabled(false);
    configProvider
        .<Integer>getConfig(instanceId + ".bulkhead.max_concurrency")
        .ifPresent(builder::maxConcurrentCalls);
    return Optional.of(builder.build());
  }
}
