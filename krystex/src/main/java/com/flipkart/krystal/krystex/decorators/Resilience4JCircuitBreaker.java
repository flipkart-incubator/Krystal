package com.flipkart.krystal.krystex.decorators;

import static io.github.resilience4j.decorators.Decorators.ofFunction;

import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.config.ConfigProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.internal.CircuitBreakerStateMachine;
import java.util.Optional;

public final class Resilience4JCircuitBreaker implements MainLogicDecorator {

  public static final String DECORATOR_TYPE = Resilience4JCircuitBreaker.class.getName();

  private final ConfigProvider configProvider;
  private final String instanceId;

  private CircuitBreaker circuitBreaker;

  /**
   * @param instanceId The tag because of which this logic decorator was applied.
   * @param configProvider The configs for this logic decorator are read from this configProvider.
   */
  public Resilience4JCircuitBreaker(String instanceId, ConfigProvider configProvider) {
    this.instanceId = instanceId;
    this.configProvider = configProvider;
    init();
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
  public void onConfigUpdate() {
    updateCircuitBreaker();
  }

  @Override
  public String getId() {
    return instanceId;
  }

  private void init() {
    this.circuitBreaker =
        getCircuitBreakerConfig()
            .map(config -> new CircuitBreakerStateMachine(instanceId + ".circuit_breaker", config))
            .orElse(null);
  }

  private Optional<CircuitBreakerConfig> getCircuitBreakerConfig() {
    boolean circuitBreakerEnabled =
        configProvider.<Boolean>getConfig(instanceId + ".circuit_breaker.enabled").orElse(true);
    if (!circuitBreakerEnabled) {
      return Optional.empty();
    }
    return Optional.of(CircuitBreakerConfig.ofDefaults());
  }

  private void updateCircuitBreaker() {
    CircuitBreaker circuitBreaker = this.circuitBreaker;
    Optional<CircuitBreakerConfig> newConfig = getCircuitBreakerConfig();
    if (!Optional.ofNullable(circuitBreaker)
        .map(CircuitBreaker::getCircuitBreakerConfig)
        .equals(newConfig)) {
      init();
    }
  }
}
