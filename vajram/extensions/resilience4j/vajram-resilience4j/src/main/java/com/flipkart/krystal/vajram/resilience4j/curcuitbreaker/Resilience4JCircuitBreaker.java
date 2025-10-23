package com.flipkart.krystal.vajram.resilience4j.curcuitbreaker;

import static com.flipkart.krystal.vajram.resilience4j.R4JUtils.decorateAsyncExecute;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Resilience4JCircuitBreaker implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JCircuitBreaker.class.getName();

  private final String instanceId;
  private @Nullable CircuitBreaker circuitBreaker;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   */
  public Resilience4JCircuitBreaker(String instanceId) {
    this.instanceId = instanceId;
  }

  public static Resilience4JCircuitBreakerManager onePerIOVajram() {
    return onePerInstanceId(logicExecutionContext -> logicExecutionContext.vajramID().id());
  }

  public static Resilience4JCircuitBreakerManager onePerInstanceId(
      Function<LogicExecutionContext, String> instanceIdGenerator) {
    return new Resilience4JCircuitBreakerManager(instanceIdGenerator);
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    CircuitBreaker circuitBreaker = this.circuitBreaker;
    if (circuitBreaker != null) {
      return input ->
          decorateAsyncExecute(logicToDecorate, input).withCircuitBreaker(circuitBreaker).get();
    } else {
      return logicToDecorate;
    }
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    updateCircuitBreaker(configProvider);
  }

  private void init(ConfigProvider configProvider) {
    this.circuitBreaker =
        getCircuitBreakerConfig(configProvider)
            .map(config -> new CircuitBreakerStateMachine(instanceId + ".circuit_breaker", config))
            .orElse(null);
  }

  private Optional<CircuitBreakerConfig> getCircuitBreakerConfig(ConfigProvider configProvider) {
    boolean circuitBreakerDisabled =
        !configProvider.<Boolean>getConfig(instanceId + ".circuit_breaker.enabled").orElse(true);
    if (circuitBreakerDisabled) {
      return Optional.empty();
    }
    return Optional.of(CircuitBreakerConfig.ofDefaults());
  }

  private void updateCircuitBreaker(ConfigProvider configProvider) {
    CircuitBreaker circuitBreaker = this.circuitBreaker;
    Optional<CircuitBreakerConfig> newConfig = getCircuitBreakerConfig(configProvider);
    if (!Optional.ofNullable(circuitBreaker)
        .map(CircuitBreaker::getCircuitBreakerConfig)
        .equals(newConfig)) {
      init(configProvider);
    }
  }
}
