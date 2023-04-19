package com.flipkart.krystal.krystex.decorators.resilience4j;

import static com.flipkart.krystal.krystex.decorators.resilience4j.R4JUtils.decorateAsyncExecute;
import static com.flipkart.krystal.krystex.decorators.resilience4j.R4JUtils.extractResponseMap;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.MainLogicDefinition;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig.Builder;
import io.github.resilience4j.bulkhead.internal.SemaphoreBulkhead;
import java.util.Optional;

public final class Resilience4JBulkhead implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JBulkhead.class.getName();

  private final String instanceId;

  private Bulkhead bulkhead;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   */
  public Resilience4JBulkhead(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public MainLogic<Object> decorateLogic(
      MainLogic<Object> logicToDecorate, MainLogicDefinition<Object> originalLogicDefinition) {
    Bulkhead bulkhead = this.bulkhead;
    if (bulkhead != null) {
      return inputsList ->
          extractResponseMap(
              inputsList,
              decorateAsyncExecute(logicToDecorate, inputsList).withBulkhead(bulkhead).get());
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

  private void init(ConfigProvider configProvider) {
    this.bulkhead =
        getBulkheadConfig(configProvider)
            .map(bulkheadConfig -> new SemaphoreBulkhead(instanceId + ".bulkhead", bulkheadConfig))
            .orElse(null);
  }

  private void updateBulkhead(ConfigProvider configProvider) {
    Bulkhead bulkhead = this.bulkhead;
    Optional<BulkheadConfig> newBulkheadConfig = getBulkheadConfig(configProvider);
    if (!Optional.ofNullable(bulkhead).map(Bulkhead::getBulkheadConfig).equals(newBulkheadConfig)) {
      if (bulkhead != null && newBulkheadConfig.isPresent()) {
        bulkhead.changeConfig(newBulkheadConfig.get());
      } else {
        init(configProvider);
      }
    }
  }

  private Optional<BulkheadConfig> getBulkheadConfig(ConfigProvider configProvider) {
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
