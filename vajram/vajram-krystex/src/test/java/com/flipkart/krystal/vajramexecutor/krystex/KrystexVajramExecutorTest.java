package com.flipkart.krystal.vajramexecutor.krystex;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.vajram.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.vajramexecutor.krystex.InputBatcherConfig.autoRegisterSharedBatchers;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.concurrent.SingleThreadExecutor;
import com.flipkart.krystal.concurrent.SingleThreadExecutorsPool;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutorConfig.KryonExecutorConfigBuilder;
import com.flipkart.krystal.krystex.kryondecoration.KryonDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecoratorConfig;
import com.flipkart.krystal.krystex.logicdecorators.observability.DefaultKryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.KryonExecutionReport;
import com.flipkart.krystal.krystex.logicdecorators.observability.MainLogicExecReporter;
import com.flipkart.krystal.krystex.logicdecorators.resilience4j.Resilience4JBulkhead;
import com.flipkart.krystal.krystex.logicdecorators.resilience4j.Resilience4JCircuitBreaker;
import com.flipkart.krystal.pooling.Lease;
import com.flipkart.krystal.pooling.LeaseUnavailableException;
import com.flipkart.krystal.vajram.annos.OutputLogicDelegationMode;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.exception.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.Hello;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.Hello_ReqImmut;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.Hello_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ReqImmut;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_ReqImmut;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello_ReqImmutPojo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService_ReqImmut;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService_ReqImmutPojo;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KrystexVajramExecutorTest {

  private static final Duration TIMEOUT = ofSeconds(1000);
  private static SingleThreadExecutorsPool EXEC_POOL;

  @BeforeAll
  static void beforeAll() {
    EXEC_POOL = new SingleThreadExecutorsPool("Test", 4);
  }

  private Lease<SingleThreadExecutor> executorLease;
  private TestRequestContext requestContext;
  private ObjectMapper objectMapper;
  private VajramKryonGraph graph;
  private LogicDecorationOrdering logicDecorationOrdering;

  @BeforeEach
  void setUp() throws LeaseUnavailableException {
    executorLease = EXEC_POOL.lease();
    logicDecorationOrdering =
        new LogicDecorationOrdering(
            ImmutableSet.of(
                Resilience4JCircuitBreaker.DECORATOR_TYPE,
                Resilience4JBulkhead.DECORATOR_TYPE,
                InputBatchingDecorator.DECORATOR_TYPE));
    requestContext = new TestRequestContext(Optional.of("user_id_1"), 2);
    objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .setSerializationInclusion(NON_NULL)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
  }

  @AfterEach
  void tearDown() {
    TestUserService.CALL_COUNTER.reset();
    FriendsService.CALL_COUNTER.reset();
    TestUserService.REQUESTS.clear();
    Hello.CALL_COUNTER.reset();
    Optional.ofNullable(graph).ifPresent(VajramKryonGraph::close);
    executorLease.close();
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_noDependencies_success(GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId("vajramWithNoDependencies")
                .build())) {
      result = krystexVajramExecutor.execute(this.helloRequest(requestContext));
    }
    assertThat(result).succeedsWithin(TIMEOUT).isEqualTo("Hello! user_id_1");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_optionalInputProvided_success(GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      result =
          krystexVajramExecutor.execute(
              helloRequestBuilder(requestContext).greeting("Namaste")._build());
    }
    assertThat(result).succeedsWithin(TIMEOUT).isEqualTo("Namaste! user_id_1");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeIo_singleRequestNoBatcher_success(GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice")
            .build();
    CompletableFuture<TestUserInfo> userInfo123;
    requestContext.requestId("ioVajramSingleRequestNoBatcher");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo123 = krystexVajramExecutor.execute(this.testUserServiceRequest(requestContext));
    }
    assertThat(userInfo123)
        .succeedsWithin(TIMEOUT)
        .extracting(TestUserInfo::userName)
        .isEqualTo("Firstname Lastname (user_id_1)");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeIo_withBatcherMultipleRequests_calledOnlyOnce(
      GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    autoRegisterSharedBatchers(graph, _v -> 3);
    CompletableFuture<String> helloString;
    requestContext.requestId("ioVajramWithBatcherMultipleRequests");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString = krystexVajramExecutor.execute(this.helloFriendsRequest(requestContext));
    }
    assertThat(helloString)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            "Hello Friends of Firstname Lastname (user_id_1)! "
                + "Firstname Lastname (user_id_1:friend_1), "
                + "Firstname Lastname (user_id_1:friend_2)");
    assertThat(TestUserService.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_sequentialDependency_success(GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(2)));
    CompletableFuture<String> helloString;
    requestContext.requestId("sequentialDependency");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString = krystexVajramExecutor.execute(this.helloFriendsV2Request(requestContext));
    }
    assertThat(helloString)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)");
    assertEquals(1, TestUserService.CALL_COUNTER.sum());
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeWithFacets_success(GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(2)));
    CompletableFuture<String> helloString;
    requestContext.requestId("sequentialDependency");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString =
          krystexVajramExecutor.execute(
              this.helloFriendsV2Request(requestContext),
              KryonExecutionConfig.builder().executionId("execution").build());
    }
    assertThat(helloString)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)");
    assertEquals(1, TestUserService.CALL_COUNTER.sum());
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_missingMandatoryInput_throwsException(
      GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      result = krystexVajramExecutor.execute(this.incompleteHelloRequest());
    }
    assertThat(result)
        .failsWithin(TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining(
            "Vajram v<"
                + graph.getVajramIdByVajramDefType(Hello.class).id()
                + "> did not receive these mandatory inputs: [ 'name'");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void execute_multiRequestNoInputBatcher_cacheHitSuccess(
      GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    CompletableFuture<TestUserInfo> userInfo;
    CompletableFuture<String> helloFriends;
    requestContext.requestId("multiRequestNoInputBatcher_cacheHitSuccess");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo =
          krystexVajramExecutor.execute(
              TestUserService_ReqImmutPojo._builder().userId("user_id_1")._build(),
              KryonExecutionConfig.builder().executionId("req_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              HelloFriends_ReqImmutPojo._builder().userId("user_id_1").numberOfFriends(0)._build(),
              KryonExecutionConfig.builder().executionId("req_2").build());
    }
    assertThat(userInfo)
        .succeedsWithin(TIMEOUT)
        .extracting(TestUserInfo::userName)
        .isEqualTo("Firstname Lastname (user_id_1)");
    assertThat(helloFriends)
        .succeedsWithin(TIMEOUT)
        .isEqualTo("Hello Friends of Firstname Lastname (user_id_1)! ");
    assertThat(TestUserService.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void execute_multiRequestWithBatcher_cacheHitSuccess(
      GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    CompletableFuture<TestUserInfo> userInfo;
    CompletableFuture<String> helloFriends;
    requestContext.requestId("ioVajramSingleRequestNoBatcher");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo =
          krystexVajramExecutor.execute(
              TestUserService_ReqImmutPojo._builder().userId("user_id_1:friend_1")._build(),
              KryonExecutionConfig.builder().executionId("req_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              HelloFriends_ReqImmutPojo._builder().userId("user_id_1").numberOfFriends(1)._build(),
              KryonExecutionConfig.builder().executionId("req_2").build());
    }
    assertThat(userInfo)
        .succeedsWithin(TIMEOUT)
        .extracting(TestUserInfo::userName)
        .isEqualTo("Firstname Lastname (user_id_1:friend_1)");
    assertThat(helloFriends)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)");
    assertThat(TestUserService.CALL_COUNTER.sum()).isEqualTo(2);
    assertThat(TestUserService.REQUESTS)
        .isEqualTo(
            Set.of(
                TestUserService_ReqImmutPojo._builder().userId("user_id_1:friend_1")._build(),
                TestUserService_ReqImmutPojo._builder().userId("user_id_1")._build()));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void execute_multiResolverFanouts_permutesTheFanouts(
      GraphTraversalStrategy graphTraversalStrategy) throws Exception {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(6)));
    CompletableFuture<String> multiHellos;
    requestContext.requestId("execute_multiResolverFanouts_permutesTheFanouts");
    KryonExecutionReport kryonExecutionReport = new DefaultKryonExecutionReport(Clock.systemUTC());
    MainLogicExecReporter mainLogicExecReporter = new MainLogicExecReporter(kryonExecutionReport);
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            KrystexVajramExecutorConfig.builder()
                .requestId(requestContext.requestId())
                .kryonExecutorConfigBuilder(
                    KryonExecutorConfig.builder()
                        .singleThreadExecutor(executorLease.get())
                        .kryonExecStrategy(BATCH)
                        .graphTraversalStrategy(graphTraversalStrategy)
                        .requestScopedLogicDecoratorConfigs(
                            ImmutableMap.of(
                                mainLogicExecReporter.decoratorType(),
                                List.of(
                                    new OutputLogicDecoratorConfig(
                                        mainLogicExecReporter.decoratorType(),
                                        (logicExecutionContext) -> true,
                                        logicExecutionContext ->
                                            mainLogicExecReporter.decoratorType(),
                                        decoratorContext -> mainLogicExecReporter)))))
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriends_ReqImmutPojo._builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  ._build());
    }
    assertThat(multiHellos)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            """
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(kryonExecutionReport));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_singleDepthParallelDependencyDefaultInputBatcherConfig_flushes2Batchers(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(100)));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriends_ReqImmutPojo._builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  ._build());
    }
    assertThat(multiHellos)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            """
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    assertThat(TestUserService.CALL_COUNTER.sum()).isEqualTo(2 /*
             Default InputBatcherConfig allocates one InputBatchingDecorator for each
             dependant call chain.
             TestUserServiceVajram is called via two dependantChains:
             [Start]>MultiHelloFriends:hellos>HelloFriendsVajram:user_info
             [Start]>MultiHelloFriends:hellos>HelloFriendsVajram:friend_infos
            */);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_singleDepthParallelDependencySharedInputBatcherConfig_flushes1Batcher(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    autoRegisterSharedBatchers(graph, _v -> 100);
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriends_ReqImmutPojo._builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  .skip(false)
                  ._build());
    }
    assertThat(multiHellos)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            """
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    assertThat(TestUserService.CALL_COUNTER.sum())
        .isEqualTo(
            /*
             TestUserServiceVajram is called via two dependantChains:
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:user_info)
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:friend_infos)
             Since input batcher is shared across these dependantChains, only one call must be
             made
            */
            1);
  }

  private KrystexVajramExecutorConfigBuilder getExecutorConfig(
      GraphTraversalStrategy graphTraversalStrategy) {
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder =
        KryonExecutorConfig.builder()
            .kryonExecStrategy(BATCH)
            .graphTraversalStrategy(graphTraversalStrategy)
            .logicDecorationOrdering(logicDecorationOrdering);

    RequestLevelCache requestLevelCache = new RequestLevelCache();
    kryonExecutorConfigBuilder.kryonDecoratorConfig(
        RequestLevelCache.DECORATOR_TYPE,
        new KryonDecoratorConfig(
            RequestLevelCache.DECORATOR_TYPE,
            executionContext -> true,
            executionContext -> RequestLevelCache.DECORATOR_TYPE,
            kryonDecoratorContext -> requestLevelCache));

    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(
            kryonExecutorConfigBuilder.singleThreadExecutor(executorLease.get()));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_singleDepthSkipParallelDependencySharedInputBatcherConfig_flushes1Batcher(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.audit",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    autoRegisterSharedBatchers(graph, _v -> 100);
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriends_ReqImmutPojo._builder()
                  .userIds(new ArrayList<>(Set.of("user_id_1", "user_id_2")))
                  .skip(true)
                  ._build());
    }
    assertThat(multiHellos).succeedsWithin(TIMEOUT).isEqualTo("");
    assertThat(TestUserService.CALL_COUNTER.sum())
        .isEqualTo(
            /*
             TestUserServiceVajram is called via two dependantChains:
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:user_infos)
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:friend_infos)
             Since we have skipped HelloFriendsVajram, we would not call TestUserSericeVajram
             so, the count should be 0.
            */
            0);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void close_sequentialDependency_flushesBatcher(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(100)));
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(FriendsService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl(100)));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriendsV2_ReqImmutPojo._builder()
                  .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                  ._build());
    }
    assertThat(multiHellos)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            """
            Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)
            Hello Friends! Firstname Lastname (user_id_2:friend1), Firstname Lastname (user_id_2:friend2)""");
    assertThat(TestUserService.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_sequentialDependency_flushesSharedBatchers(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello")
            .build();
    autoRegisterSharedBatchers(graph, vajramId -> 100);
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MutualFriendsHello_ReqImmutPojo._builder().userId("user_id_1")._build());
    }
    assertThat(multiHellos)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            """
            Hello Friends! Firstname Lastname (user_id_1:friend1:friend1), Firstname Lastname (user_id_1:friend1:friend2)
            Hello Friends! Firstname Lastname (user_id_1:friend2:friend1), Firstname Lastname (user_id_1:friend2:friend2)""");
    assertThat(FriendsService.CALL_COUNTER.sum()).isEqualTo(2);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_sequentialSkipDependency_flushesSharedBatchers(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello")
            .build();
    autoRegisterSharedBatchers(graph, _v -> 100);
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MutualFriendsHello_ReqImmutPojo._builder().userId("user_id_1").skip(true)._build());
    }
    assertThat(multiHellos).succeedsWithin(1, TimeUnit.HOURS).isEqualTo("");
    assertThat(FriendsService.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_skippingADependency_flushesCompleteCallGraph(
      GraphTraversalStrategy graphTraversalStrategy, TestInfo testInfo) {
    CompletableFuture<FlushCommand> friendServiceFlushCommand = new CompletableFuture<>();
    CompletableFuture<FlushCommand> userServiceFlushCommand = new CompletableFuture<>();
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2")
            .build();
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(FriendsService.class),
        new InputBatcherConfig(
            logicExecutionContext -> "",
            _x -> true,
            batcherContext ->
                new OutputLogicDecorator() {
                  @Override
                  public OutputLogic<Object> decorateLogic(
                      OutputLogic<Object> logicToDecorate,
                      OutputLogicDefinition<Object> originalLogicDefinition) {
                    return logicToDecorate;
                  }

                  @Override
                  public void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {
                    if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
                      friendServiceFlushCommand.complete(flushCommand);
                    }
                  }

                  @Override
                  public String getId() {
                    return "friendService";
                  }
                }));
    graph.registerInputBatchers(
        graph.getVajramIdByVajramDefType(TestUserService.class),
        new InputBatcherConfig(
            logicExecutionContext1 -> "1",
            _x -> true,
            batcherContext ->
                new OutputLogicDecorator() {
                  @Override
                  public OutputLogic<Object> decorateLogic(
                      OutputLogic<Object> logicToDecorate,
                      OutputLogicDefinition<Object> originalLogicDefinition) {
                    return logicToDecorate;
                  }

                  @Override
                  public void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {
                    if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
                      userServiceFlushCommand.complete(flushCommand);
                    }
                  }

                  @Override
                  public String getId() {
                    return "userService";
                  }
                }));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              MultiHelloFriendsV2_ReqImmutPojo._builder()
                  .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                  .skip(true)
                  ._build());
    }
    assertThat(friendServiceFlushCommand).succeedsWithin(TIMEOUT);
    assertThat(userServiceFlushCommand).succeedsWithin(TIMEOUT);
    assertThat(multiHellos).succeedsWithin(TIMEOUT).isEqualTo("");
  }

  private Hello_ReqImmut helloRequest(TestRequestContext applicationRequestContext) {
    return helloRequestBuilder(applicationRequestContext)._build();
  }

  private Hello_ReqImmut.Builder helloRequestBuilder(TestRequestContext applicationRequestContext) {
    return Hello_ReqImmutPojo._builder()
        .name(applicationRequestContext.loggedInUserId().orElseThrow());
  }

  private Hello_ReqImmut incompleteHelloRequest() {
    return Hello_ReqImmutPojo._builder()._build();
  }

  private TestUserService_ReqImmut testUserServiceRequest(TestRequestContext testRequestContext) {
    return TestUserService_ReqImmutPojo._builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        ._build();
  }

  private HelloFriends_ReqImmut helloFriendsRequest(TestRequestContext testRequestContext) {
    return HelloFriends_ReqImmutPojo._builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        .numberOfFriends(testRequestContext.numberOfFriends())
        ._build();
  }

  private HelloFriendsV2_ReqImmut helloFriendsV2Request(TestRequestContext testRequestContext) {
    return HelloFriendsV2_ReqImmutPojo._builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        ._build();
  }

  private static VajramKryonGraphBuilder loadFromClasspath(String... packagePrefixes) {
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    Predicate<LogicExecutionContext> isIOVajram =
        (context) ->
            context
                .kryonDefinitionRegistry()
                .getOrThrow(context.vajramID())
                .tags()
                .getAnnotationByType(OutputLogicDelegationMode.class)
                .map(v -> v.value() == SYNC)
                .orElse(false);
    Function<LogicExecutionContext, String> instanceIdCreator = context -> context.vajramID().id();
    return builder
        .decorateOutputLogicForSession(
            new OutputLogicDecoratorConfig(
                Resilience4JBulkhead.DECORATOR_TYPE,
                isIOVajram,
                instanceIdCreator,
                decoratorContext ->
                    new Resilience4JBulkhead(
                        instanceIdCreator.apply(decoratorContext.logicExecutionContext()))))
        .decorateOutputLogicForSession(
            new OutputLogicDecoratorConfig(
                Resilience4JCircuitBreaker.DECORATOR_TYPE,
                isIOVajram,
                instanceIdCreator,
                decoratorContext ->
                    new Resilience4JCircuitBreaker(
                        instanceIdCreator.apply(decoratorContext.logicExecutionContext()))));
  }

  public static Stream<Arguments> executorConfigsToTest() {
    return Stream.of(Arguments.of(DEPTH), Arguments.of(BREADTH));
  }
}
