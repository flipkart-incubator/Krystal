package com.flipkart.krystal.krystex.decorators;

import static io.github.resilience4j.decorators.Decorators.ofFunction;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import java.util.Optional;

public final class Resilience4JCircuitBreaker implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JCircuitBreaker.class.getName();

  private final String instanceId;

  private CircuitBreaker circuitBreaker;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   */
  public Resilience4JCircuitBreaker(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public MainLogic<Object> decorateLogic(MainLogic<Object> logicToDecorate) {
    CircuitBreaker circuitBreaker = this.circuitBreaker;
    if (circuitBreaker != null) {
      return ofFunction(logicToDecorate::execute).withCircuitBreaker(circuitBreaker)::apply;
    } else {
      return logicToDecorate;
    }
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    updateCircuitBreaker(configProvider);
  }

  @Override
  public String getId() {
    return instanceId;
  }

  private void init(ConfigProvider configProvider) {
    this.circuitBreaker =
        getCircuitBreakerConfig(configProvider)
            .map(config -> new CircuitBreakerStateMachine(instanceId + ".circuit_breaker", config))
            .orElse(null);
  }

  private Optional<CircuitBreakerConfig> getCircuitBreakerConfig(ConfigProvider configProvider) {
    boolean circuitBreakerEnabled =
        configProvider.<Boolean>getConfig(instanceId + ".circuit_breaker.enabled").orElse(true);
    if (!circuitBreakerEnabled) {
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
