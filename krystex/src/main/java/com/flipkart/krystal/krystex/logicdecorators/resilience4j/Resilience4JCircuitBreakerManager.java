package com.flipkart.krystal.krystex.logicdecorators.resilience4j;

import static com.flipkart.krystal.krystex.logicdecorators.resilience4j.Resilience4JCircuitBreaker.DECORATOR_TYPE;
import static java.util.Collections.synchronizedList;

import com.flipkart.krystal.annos.ComputeDelegationMode;
import com.flipkart.krystal.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfigurator;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class Resilience4JCircuitBreakerManager implements KryonExecutorConfigurator {

  private final ConcurrentHashMap<String, Resilience4JCircuitBreaker> circuitBreakers =
      new ConcurrentHashMap<>();

  private final Function<LogicExecutionContext, String> instanceIdGenerator;
  private final List<Consumer<Resilience4JCircuitBreaker>> listeners =
      synchronizedList(new ArrayList<>());

  Resilience4JCircuitBreakerManager(Function<LogicExecutionContext, String> instanceIdGenerator) {
    this.instanceIdGenerator = instanceIdGenerator;
  }

  @Override
  public void addToConfig(KryonExecutorConfigBuilder configBuilder) {
    configBuilder.outputLogicDecoratorConfig(
        DECORATOR_TYPE,
        new OutputLogicDecoratorConfig(
            DECORATOR_TYPE,
            logicExecutionContext ->
                logicExecutionContext
                        .logicTags()
                        .getAnnotationByType(OutputLogicDelegationMode.class)
                        .map(OutputLogicDelegationMode::value)
                        .orElse(ComputeDelegationMode.NONE)
                    != ComputeDelegationMode.NONE,
            instanceIdGenerator,
            logicDecoratorContext ->
                circuitBreakers.computeIfAbsent(
                    instanceIdGenerator.apply(logicDecoratorContext.logicExecutionContext()),
                    instanceId -> {
                      Resilience4JCircuitBreaker circuitBreaker =
                          new Resilience4JCircuitBreaker(instanceId);
                      listeners.forEach(l -> l.accept(circuitBreaker));
                      return circuitBreaker;
                    })));
  }

  public ImmutableMap<String, Resilience4JCircuitBreaker> circuitBreakers() {
    return ImmutableMap.copyOf(circuitBreakers);
  }

  public void onCreate(Consumer<Resilience4JCircuitBreaker> listener) {
    listeners.add(listener);
  }
}
