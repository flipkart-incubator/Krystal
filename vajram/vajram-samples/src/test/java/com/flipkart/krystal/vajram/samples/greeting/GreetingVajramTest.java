package com.flipkart.krystal.vajram.samples.greeting;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.google.inject.Guice.createInjector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjectionProvider;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.InputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GreetingVajramTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  private Builder graph;
  private ObjectMapper objectMapper;
  private static final String USER_ID = "user@123";
  private static final String USER_NAME = "Ranchhoddas Shyamakdas Chanchhad";
  private static final String REQUEST_ID = "greetingRequest1";
  private static final String PACKAGE_PATH = "com.flipkart.krystal.vajram.samples.greeting";

  @BeforeEach
  public void setUp() {
    graph =
        new VajramKryonGraph.Builder()
            .loadFromPackage(PACKAGE_PATH)
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
  void greetingVajram_success() throws Exception {
    CompletableFuture<String> future;
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                new RequestContext(REQUEST_ID, USER_ID),
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
    assertThat(future.get()).contains(USER_ID);
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

  public record RequestContext(String requestId, String userId)
      implements ApplicationRequestContext {}

  private static CompletableFuture<String> executeVajram(
      KrystexVajramExecutor<RequestContext> krystexVajramExecutor) {
    return krystexVajramExecutor.execute(
        vajramID(getVajramIdString(Greeting.class)),
        rc -> GreetingRequest.builder().userId(rc.userId).build(),
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

  @Test
  void greeting_success_when_user_service_call_is_success() {
    CompletableFuture<String> future;
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder =
        KryonExecutorConfig.builder()
            .kryonExecStrategy(KryonExecStrategy.BATCH)
            .graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                new RequestContext(REQUEST_ID, USER_ID),
                VajramTestHarness.prepareForTest(kryonExecutorConfigBuilder)
                    .withMock(
                        UserServiceRequest.builder().userId(USER_ID).build(),
                        withValue(new UserInfo(USER_ID, USER_NAME)))
                    .buildConfig())) {
      future = executeVajram(krystexVajramExecutor);
    }
    assertThat(future).succeedsWithin(TIMEOUT).asInstanceOf(STRING).contains(USER_NAME);
  }

  @Test
  void greeting_success_when_user_service_fails_with_request_timeout() {
    CompletableFuture<String> future;
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder =
        KryonExecutorConfig.builder()
            .kryonExecStrategy(KryonExecStrategy.BATCH)
            .graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                new RequestContext(REQUEST_ID, USER_ID),
                VajramTestHarness.prepareForTest(kryonExecutorConfigBuilder)
                    .withMock(
                        UserServiceRequest.builder().userId(USER_ID).build(),
                        ValueOrError.withError(new IOException("Request Timeout")))
                    .buildConfig())) {
      future = executeVajram(krystexVajramExecutor);
    }
    assertThat(future).succeedsWithin(TIMEOUT).asInstanceOf(STRING).doesNotContain(USER_NAME);
  }
}
