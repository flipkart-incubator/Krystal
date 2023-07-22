package com.flipkart.krystal.vajram.samples.greeting;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.google.inject.Guice.createInjector;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.decorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.krystex.decorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjector;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GreetingVajramTest {
  private Builder graph;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    graph =
        new VajramKryonGraph.Builder()
            .loadFromPackage("com.flipkart.krystal.vajram.samples.greeting")
            .injectInputsWith(wrapInjector(createInjector(new GuiceModule())))
            .logicDecorationOrdering(
                new LogicDecorationOrdering(
                    ImmutableSet.<String>builder()
                        .add(InputInjector.DECORATOR_TYPE)
                        .add(MainLogicExecReporter.class.getName())
                        .build()));

    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @Test
  public void greetingVajram_success() throws Exception {
    CompletableFuture<String> future;
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph
            .build()
            .createExecutor(
                new RequestContext("greetingTest"),
                KryonExecutorConfig.builder()
                    .requestScopedLogicDecoratorConfigs(
                        ImmutableMap.of(
                            mainLogicExecReporter.decoratorType(),
                            List.of(
                                new MainLogicDecoratorConfig(
                                    mainLogicExecReporter.decoratorType(),
                                    logicExecutionContext -> true,
                                    logicExecutionContext -> mainLogicExecReporter.decoratorType(),
                                    decoratorContext -> mainLogicExecReporter))))
                    .build())) {
      future = executeVajram(krystexVajramExecutor);
    }
    assertThat(future.get()).contains("user@123");
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  private static class GuiceModule extends AbstractModule {

    @Provides
    @Singleton
    @Named("analytics_sink")
    public AnalyticsEventSink provideAnalyticsEventSink() {
      return new AnalyticsEventSink();
    }

    @Provides
    @Singleton
    public System.Logger providesLogger() {
      return System.getLogger("greetingLogger");
    }
  }

  record RequestContext(String requestId) implements ApplicationRequestContext {}

  private static CompletableFuture<String> executeVajram(
      KrystexVajramExecutor<RequestContext> krystexVajramExecutor) {
    return krystexVajramExecutor.execute(
        vajramID(GreetingVajram.ID),
        rc -> GreetingRequest.builder().userId("user@123").build(),
        KryonExecutionConfig.builder().executionId("req_1").build());
  }

  private static InputInjectionProvider wrapInjector(Injector injector) {
    return new InputInjectionProvider() {

      public Object getInstance(Class<?> clazz) {
        return injector.getInstance(clazz);
      }

      public Object getInstance(Class<?> clazz, String injectionName) {
        return injector.getInstance(Key.get(clazz, Names.named(injectionName)));
      }
    };
  }
}
