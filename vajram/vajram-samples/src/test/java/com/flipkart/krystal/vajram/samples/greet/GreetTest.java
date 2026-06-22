package com.flipkart.krystal.vajram.samples.greet;

import static com.fasterxml.jackson.annotation.JsonInclude.Value.ALL_NON_NULL;
import static com.flipkart.krystal.data.Errable.withValue;
import static com.google.inject.Guice.createInjector;
import static java.lang.System.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.krystex.KrystalExecutorConfig;
import com.flipkart.krystal.krystex.KrystalExecutorConfig.KrystalExecutorConfigBuilder;
import com.flipkart.krystal.krystex.KrystexGraph;
import com.flipkart.krystal.krystex.KrystexGraph.KrystexGraphBuilder;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.caching.TestRequestLevelCache;
import com.flipkart.krystal.krystex.decoration.DecorationOrdering;
import com.flipkart.krystal.krystex.inputinjection.KryonInputInjector;
import com.flipkart.krystal.krystex.kryon.VajramExecutionConfig;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor;
import com.flipkart.krystal.krystex.kryon.VajramKryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.testharness.VajramTestHarness;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.guice.injection.VajramGuiceInputInjector;
import com.flipkart.krystal.visualization.executiongraph.DefaultKryonExecutionReport;
import com.flipkart.krystal.visualization.executiongraph.KryonExecutionReport;
import com.flipkart.krystal.visualization.executiongraph.MainLogicExecReporter;
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

  private VajramGraph graph;
  private ObjectMapper objectMapper;
  private static final String USER_ID = "user@123";

  @SuppressWarnings("SpellCheckingInspection")
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
    this.injector = createInjector(new GuiceModule());
    this.decorationOrdering =
        new DecorationOrdering(
            ImmutableSet.<String>builder()
                // Output logic decorators
                .add(MainLogicExecReporter.DECORATOR_TYPE)
                // KryonDecorators
                .add(RequestLevelCache.KRYON_DECORATOR_TYPE)
                .add(KryonInputInjector.DECORATOR_TYPE)
                .build());
    this.graph = VajramGraph.builder().loadFromPackage(PACKAGE_PATH).build();
    this.requestLevelCache = new TestRequestLevelCache(graph);

    this.objectMapper =
        JsonMapper.builder()
            .addModules(new JavaTimeModule(), new Jdk8Module())
            .defaultPropertyInclusion(ALL_NON_NULL)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .addModule(
                new SimpleModule()
                    .addSerializer(
                        new StdSerializer<>(Logger.class) {
                          @Override
                          public void serialize(
                              Logger value, JsonGenerator gen, SerializerProvider serializers)
                              throws IOException {
                            gen.writeString(value.toString());
                          }
                        }))
            .build();
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
    KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(graph);
    kGraph.injectionProvider(new VajramGuiceInputInjector(injector));
    try (VajramKryonExecutor krystexVajramExecutor =
        kGraph
            .build()
            .createExecutor(
                KrystalExecutorConfig.builder()
                    .executorId(REQUEST_ID)
                    .executorService(executorLease.get())
                    .decorationOrdering(decorationOrdering)
                    .configureWith(
                        new MainLogicExecReporter(kryonExecutionReport)
                            .defaultKryonExecutorConfigurator()))) {
      future = executeVajram(krystexVajramExecutor, requestContext);
    }
    assertThat(future)
        .succeedsWithin(TIMEOUT)
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
      VajramKryonExecutor krystexVajramExecutor, RequestContext rc) {
    return krystexVajramExecutor.execute(
        Greet_ReqImmutPojo._builder().userId(rc.userId)._build(),
        VajramExecutionConfig.builder().executionId("req_1").build());
  }

  @Test
  void greeting_success_when_user_service_call_is_success() {
    CompletableFuture<String> future;
    KrystalExecutorConfigBuilder executorConfig =
        KrystalExecutorConfig.builder()
            .executorId(REQUEST_ID)
            .executorService(executorLease.get())
            .graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    RequestContext requestContext = new RequestContext(REQUEST_ID, USER_ID);
    VajramGraph vajramGraph = graph;
    KrystexGraphBuilder kGraph = KrystexGraph.builder().vajramGraph(vajramGraph);
    kGraph.injectionProvider(new VajramGuiceInputInjector(injector));
    try (VajramKryonExecutor krystexVajramExecutor =
        kGraph
            .build()
            .createExecutor(
                VajramTestHarness.prepareForTest(executorConfig, requestLevelCache)
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
    KrystalExecutorConfigBuilder executorConfig =
        KrystalExecutorConfig.builder()
            .executorId(REQUEST_ID)
            .executorService(executorLease.get())
            .graphTraversalStrategy(GraphTraversalStrategy.DEPTH);
    RequestContext requestContext = new RequestContext(REQUEST_ID, USER_ID);
    VajramGraph vajramGraph = graph;
    KrystexGraphBuilder krystexGraph = KrystexGraph.builder().vajramGraph(vajramGraph);
    krystexGraph.injectionProvider(new VajramGuiceInputInjector(injector));
    try (VajramKryonExecutor krystexVajramExecutor =
        krystexGraph
            .build()
            .createExecutor(
                VajramTestHarness.prepareForTest(executorConfig, requestLevelCache)
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
