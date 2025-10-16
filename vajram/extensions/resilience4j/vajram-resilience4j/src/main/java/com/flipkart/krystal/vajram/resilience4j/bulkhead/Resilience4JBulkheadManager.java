package com.flipkart.krystal.vajram.resilience4j.bulkhead;

import static com.flipkart.krystal.vajram.resilience4j.bulkhead.Resilience4JBulkhead.DECORATOR_TYPE;
import static java.util.Collections.synchronizedList;

import com.flipkart.krystal.annos.ComputeDelegationMode;
import com.flipkart.krystal.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.krystex.kryon.KryonDefinition;
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

public class Resilience4JBulkheadManager implements KryonExecutorConfigurator {

  private final ConcurrentHashMap<String, Resilience4JBulkhead> bulkheads =
      new ConcurrentHashMap<>();

  private final Function<LogicExecutionContext, String> instanceIdGenerator;
  private final List<Consumer<Resilience4JBulkhead>> listeners =
      synchronizedList(new ArrayList<>());

  Resilience4JBulkheadManager(Function<LogicExecutionContext, String> instanceIdGenerator) {
    this.instanceIdGenerator = instanceIdGenerator;
  }

  @Override
  public void addToConfig(KryonExecutorConfigBuilder configBuilder) {
    configBuilder.outputLogicDecoratorConfig(
        DECORATOR_TYPE,
        new OutputLogicDecoratorConfig(
            DECORATOR_TYPE,
            logicExecutionContext -> {
              KryonDefinition kryonDefinition =
                  logicExecutionContext
                      .kryonDefinitionRegistry()
                      .get(logicExecutionContext.vajramID());
              if (kryonDefinition == null) {
                return false;
              }
              return kryonDefinition
                      .tags()
                      .getAnnotationByType(OutputLogicDelegationMode.class)
                      .map(OutputLogicDelegationMode::value)
                      .orElse(ComputeDelegationMode.NONE)
                  != ComputeDelegationMode.NONE;
            },
            instanceIdGenerator,
            logicDecoratorContext ->
                bulkheads.computeIfAbsent(
                    instanceIdGenerator.apply(logicDecoratorContext.logicExecutionContext()),
                    instanceId -> {
                      Resilience4JBulkhead resilience4JBulkhead =
                          new Resilience4JBulkhead(instanceId);
                      listeners.forEach(l -> l.accept(resilience4JBulkhead));
                      return resilience4JBulkhead;
                    })));
  }

  public ImmutableMap<String, Resilience4JBulkhead> bulkheads() {
    return ImmutableMap.copyOf(bulkheads);
  }

  public void onCreate(Consumer<Resilience4JBulkhead> listener) {
    listeners.add(listener);
  }
}
