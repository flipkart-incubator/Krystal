package com.flipkart.krystal.vajram.samples.greet;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.data.Errable.withValue;
import static com.google.inject.Guice.createInjector;
import static java.lang.System.*;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.inputinjection.VajramGuiceInputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.inputinjection.KryonInputInjector;
import com.flipkart.krystal.vajramexecutor.krystex.testharness.VajramTestHarness;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.io.IOException;
import java.lang.System.Logger;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GreetTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private VajramKryonGraphBuilder graph;
  private ObjectMapper objectMapper;
  private static final String USER_ID = "user@123";
  private static final String USER_NAME = "Ranchoddas Shamaldas Chanchad";
  private static final String REQUEST_ID = "greetingRequest1";
  private static final String PACKAGE_PATH = Greet.class.getPackageName();

  private final MyAnalyticsEventSinkImpl analyticsEventSink = new MyAnalyticsEventSinkImpl();

  private DecorationOrdering decorationOrdering;
  private TestRequestLevelCache requestLevelCache;
  private Injector injector;

  private Lease<SingleThreadExecutor> executorLease;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    this.executorLease = EXEC_POOL.lease();
    injector = createInjector(new GuiceModule());
    requestLevelCache = new TestRequestLevelCache();
    decorationOrdering =
        new DecorationOrdering(
            ImmutableSet.<String>builder()
                // Output logic decorators
                .add(MainLogicExecReporter.DECORATOR_TYPE)
                // KryonDecorators
                .add(RequestLevelCache.DECORATOR_TYPE)
                .add(KryonInputInjector.DECORATOR_TYPE)
                .build());
    graph = VajramKryonGraph.builder().loadFromPackage(PACKAGE_PATH);

    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .registerModule(
                new SimpleModule()
                    .addSerializer(
                        new StdSerializer<>(Logger.class) {
                          @Override
                          public void serialize(
                              Logger value, JsonGenerator gen, SerializerProvider serializers)
                              throws IOException {
                            gen.writeString(value.toString());
                          }
                        }));
  }

  @AfterEach
  void tearDown() {
    executorLease.close();
  }

  @Test
  void greetingVajram_success() throws Exception {
    CompletableFuture<String> future;
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    RequestContext requestContext = new RequestContext(REQUEST_ID, USER_ID);
    assertThat(analyticsEventSink.events).isEmpty();
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                KrystexVajramExecutorConfig.builder()
                    .requestId(REQUEST_ID)
                    .inputInjectionProvider(new VajramGuiceInputInjector(injector))
                    .kryonExecutorConfigBuilder(
                        KryonExecutorConfig.builder()
                            .singleThreadExecutor(executorLease.get())
                            .decorationOrdering(decorationOrdering)
                            .configureWith(new MainLogicExecReporter(kryonExecutionReport)))
                    .build())) {
      future = executeVajram(krystexVajramExecutor, requestContext);
    }
    assertThat(future)
        .succeedsWithin(1, SECONDS)
        .isEqualTo("Hello Firstname Lastname (user@123)! Hope you are doing well!");
    assertThat(analyticsEventSink.events).hasSize(1);
    out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  private class GuiceModule extends AbstractModule {

    @Provides
    @Singleton
    @Named("analytics_sink")
    public AnalyticsEventSink provideAnalyticsEventSink() {
      return analyticsEventSink;
    }

    @Provides
    @Singleton
    public Logger providesLogger() {
      return getLogger("greetingLogger");
    }
  }

  public record RequestContext(String requestId, String userId) {}

  private static CompletableFuture<String> executeVajram(
      KrystexVajramExecutor krystexVajramExecutor, RequestContext rc) {
    return krystexVajramExecutor.execute(
        Greet_ReqImmutPojo._builder().userId(rc.userId)._build(),
        KryonExecutionConfig.builder().executionId("req_1").build());
  }

  @Test
  void greeting_success_when_user_service_call_is_success() {
    CompletableFuture<String> future;
    KrystexVajramExecutorConfigBuilder executorConfig =
        KrystexVajramExecutorConfig.builder()
            .requestId(REQUEST_ID)
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .singleThreadExecutor(executorLease.get())
                    .kryonExecStrategy(KryonExecStrategy.BATCH)
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH));
    RequestContext requestContext = new RequestContext(REQUEST_ID, USER_ID);
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                VajramTestHarness.prepareForTest(
                        executorConfig
                            .inputInjectionProvider(new VajramGuiceInputInjector(injector))
                            .build(),
                        requestLevelCache)
                    .withMock(
                        UserService_FacImmutPojo._builder().userId(USER_ID)._build(),
                        withValue(new UserInfo(USER_ID, USER_NAME)))
                    .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, requestContext);
    }
    assertThat(future).succeedsWithin(TIMEOUT).asInstanceOf(STRING).contains(USER_NAME);
  }

  @Test
  void greeting_success_when_user_service_fails_with_request_timeout() {
    CompletableFuture<String> future;
    KrystexVajramExecutorConfigBuilder executorConfig =
        KrystexVajramExecutorConfig.builder()
            .requestId(REQUEST_ID)
            .kryonExecutorConfigBuilder(
                KryonExecutorConfig.builder()
                    .singleThreadExecutor(executorLease.get())
                    .kryonExecStrategy(KryonExecStrategy.BATCH)
                    .graphTraversalStrategy(GraphTraversalStrategy.DEPTH));
    RequestContext requestContext = new RequestContext(REQUEST_ID, USER_ID);
    try (VajramKryonGraph vajramKryonGraph = graph.build();
        KrystexVajramExecutor krystexVajramExecutor =
            vajramKryonGraph.createExecutor(
                VajramTestHarness.prepareForTest(
                        executorConfig
                            .inputInjectionProvider(new VajramGuiceInputInjector(injector))
                            .build(),
                        requestLevelCache)
                    .withMock(
                        UserService_FacImmutPojo._builder().userId(USER_ID)._build(),
                        Errable.withError(new IOException("Request Timeout")))
                    .buildConfig())) {
      future = executeVajram(krystexVajramExecutor, requestContext);
    }
    assertThat(future).succeedsWithin(TIMEOUT).asInstanceOf(STRING).doesNotContain(USER_NAME);
  }

  private static class MyAnalyticsEventSinkImpl implements AnalyticsEventSink {

    private final List<GreetingEvent> events = new ArrayList<>();

    @Override
    public void pushEvent(String eventType, GreetingEvent greetingEvent) {
      events.add(greetingEvent);
    }
  }
}
