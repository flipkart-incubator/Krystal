package com.flipkart.krystal.vajramexecutor.krystex;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.BREADTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy.DEPTH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.BATCH;
import static com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy.GRANULAR;
import static com.flipkart.krystal.vajram.VajramID.ofVajram;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;
import static com.flipkart.krystal.vajram.tags.AnnotationTags.getAnnotationByType;
import static com.flipkart.krystal.vajram.tags.AnnotationTags.getNamedValueTag;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.caching.RequestLevelCache;
import com.flipkart.krystal.krystex.kryon.KryonExecutionConfig;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.GraphTraversalStrategy;
import com.flipkart.krystal.krystex.kryon.KryonExecutor.KryonExecStrategy;
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
import com.flipkart.krystal.vajram.MandatoryFacetsMissingException;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.flipkart.krystal.vajram.tags.NamedValueTag;
import com.flipkart.krystal.vajram.tags.Service;
import com.flipkart.krystal.vajram.tags.ServiceApi;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.flipkart.krystal.vajram.tags.VajramTags.VajramTypes;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutorConfig.KrystexVajramExecutorConfigBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.Hello;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHello;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello.MutualFriendsHelloRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserService;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KrystexVajramExecutorTest {

  private static final Duration TIMEOUT = ofSeconds(1);
  private TestRequestContext requestContext;
  private ObjectMapper objectMapper;
  private VajramKryonGraph graph;
  private LogicDecorationOrdering logicDecorationOrdering;

  @BeforeEach
  void setUp() {
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
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @AfterEach
  void tearDown() {
    TestUserService.CALL_COUNTER.reset();
    FriendsService.CALL_COUNTER.reset();
    TestUserService.REQUESTS.clear();
    Hello.CALL_COUNTER.reset();
    Optional.ofNullable(graph).ifPresent(VajramKryonGraph::close);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_noDependencies_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId("vajramWithNoDependencies")
                .build())) {
      result =
          krystexVajramExecutor.execute(ofVajram(Hello.class), this.helloRequest(requestContext));
    }
    assertThat(result).succeedsWithin(TIMEOUT).isEqualTo("Hello! user_id_1");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_optionalInputProvided_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      result =
          krystexVajramExecutor.execute(
              ofVajram(Hello.class),
              helloRequestBuilder(requestContext).greeting("Namaste").build());
    }
    assertThat(result).succeedsWithin(TIMEOUT).isEqualTo("Namaste! user_id_1");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeIo_singleRequestNoBatcher_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice")
            .build();
    CompletableFuture<TestUserInfo> userInfo123;
    requestContext.requestId("ioVajramSingleRequestNoBatcher");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo123 =
          krystexVajramExecutor.execute(
              ofVajram(TestUserService.class), this.testUserServiceRequest(requestContext));
    }
    assertThat(userInfo123)
        .succeedsWithin(TIMEOUT)
        .extracting(TestUserInfo::userName)
        .isEqualTo("Firstname Lastname (user_id_1)");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeIo_withBatcherMultipleRequests_calledOnlyOnce(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(3),
            getVajramIdString(TestUserService.class) + "Batcher",
            graph.computeDependantChain(getVajramIdString(HelloFriends.class), "userInfo"),
            graph.computeDependantChain(getVajramIdString(HelloFriends.class), "friendInfos")));

    CompletableFuture<String> helloString;
    requestContext.requestId("ioVajramWithBatcherMultipleRequests");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString =
          krystexVajramExecutor.execute(
              ofVajram(HelloFriends.class), this.helloFriendsRequest(requestContext));
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
  void executeCompute_sequentialDependency_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(2)));
    CompletableFuture<String> helloString;
    requestContext.requestId("sequentialDependency");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString =
          krystexVajramExecutor.execute(
              ofVajram(HelloFriendsV2.class), this.helloFriendsV2Request(requestContext));
    }
    assertThat(helloString)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            ("Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)"));
    assertEquals(1, TestUserService.CALL_COUNTER.sum());
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeWithFacets_success(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(2)));
    CompletableFuture<String> helloString;
    requestContext.requestId("sequentialDependency");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      helloString =
          krystexVajramExecutor.executeWithFacets(
              ofVajram(HelloFriendsV2.class),
              helloFriendsV2Request(requestContext).toFacetValues(),
              KryonExecutionConfig.builder().executionId("execution").build());
    }
    assertThat(helloString)
        .succeedsWithin(TIMEOUT)
        .isEqualTo(
            ("Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)"));
    assertEquals(1, TestUserService.CALL_COUNTER.sum());
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void executeCompute_missingMandatoryInput_throwsException(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      result = krystexVajramExecutor.execute(ofVajram(Hello.class), this.incompleteHelloRequest());
    }
    assertThat(result)
        .failsWithin(TIMEOUT)
        .withThrowableOfType(ExecutionException.class)
        .withCauseExactlyInstanceOf(MandatoryFacetsMissingException.class)
        .withMessageContaining(
            "Vajram v<"
                + getVajramIdString(Hello.class)
                + "> did not receive these mandatory inputs: [ name");
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void execute_multiRequestNoInputBatcher_cacheHitSuccess(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
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
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo =
          krystexVajramExecutor.execute(
              ofVajram(TestUserService.class),
              TestUserServiceRequest.builder().userId("user_id_1").build(),
              KryonExecutionConfig.builder().executionId("req_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              ofVajram(HelloFriends.class),
              HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(0).build(),
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
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
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
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      userInfo =
          krystexVajramExecutor.execute(
              ofVajram(TestUserService.class),
              TestUserServiceRequest.builder().userId("user_id_1:friend_1").build(),
              KryonExecutionConfig.builder().executionId("req_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              ofVajram(HelloFriends.class),
              HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(1).build(),
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
                TestUserServiceRequest.builder().userId("user_id_1:friend_1").build(),
                TestUserServiceRequest.builder().userId("user_id_1").build()));
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void execute_multiResolverFanouts_permutesTheFanouts(
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy)
      throws Exception {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(6)));
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
                        .kryonExecStrategy(kryonExecStrategy)
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
              ofVajram(MultiHelloFriends.class),
              MultiHelloFriendsRequest.builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  .build());
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
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(100)));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MultiHelloFriends.class),
              MultiHelloFriendsRequest.builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  .build());
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
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(TestUserService.class) + "Batcher",
            graph.computeDependantChain(
                getVajramIdString(MultiHelloFriends.class), "hellos", "userInfo"),
            graph.computeDependantChain(
                getVajramIdString(MultiHelloFriends.class), "hellos", "friendInfos")));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MultiHelloFriends.class),
              MultiHelloFriendsRequest.builder()
                  .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                  .skip(false)
                  .build());
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
      KryonExecStrategy kryonExecStrategy, GraphTraversalStrategy graphTraversalStrategy) {
    KryonExecutorConfigBuilder kryonExecutorConfigBuilder =
        KryonExecutorConfig.builder()
            .kryonExecStrategy(kryonExecStrategy)
            .graphTraversalStrategy(graphTraversalStrategy)
            .logicDecorationOrdering(logicDecorationOrdering);
    if (BATCH.equals(kryonExecStrategy)) {
      RequestLevelCache requestLevelCache = new RequestLevelCache();
      kryonExecutorConfigBuilder.requestScopedKryonDecoratorConfig(
          RequestLevelCache.DECORATOR_TYPE,
          new KryonDecoratorConfig(
              RequestLevelCache.DECORATOR_TYPE,
              executionContext -> true,
              executionContext -> RequestLevelCache.DECORATOR_TYPE,
              kryonDecoratorContext -> {
                return requestLevelCache;
              }));
    }
    return KrystexVajramExecutorConfig.builder()
        .kryonExecutorConfigBuilder(kryonExecutorConfigBuilder);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_singleDepthSkipParallelDependencySharedInputBatcherConfig_flushes1Batcher(
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(TestUserService.class) + "Batcher",
            graph.computeDependantChain(
                getVajramIdString(MultiHelloFriends.class), "hellos", "user_infos"),
            graph.computeDependantChain(
                getVajramIdString(MultiHelloFriends.class), "hellos", "friend_infos")));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MultiHelloFriends.class),
              MultiHelloFriendsRequest.builder()
                  .userIds(new ArrayList<>(Set.of("user_id_1", "user_id_2")))
                  .skip(true)
                  .build());
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
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(100)));
    graph.registerInputBatchers(
        ofVajram(FriendsService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(100)));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MultiHelloFriendsV2.class),
              MultiHelloFriendsV2Request.builder()
                  .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                  .build());
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
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(100)));
    graph.registerInputBatchers(
        ofVajram(FriendsService.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(FriendsService.class) + "_1",
            graph.computeDependantChain(
                getVajramIdString(MutualFriendsHello.class), "hellos", "friendIds")),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(FriendsService.class) + "_2",
            graph.computeDependantChain(getVajramIdString(MutualFriendsHello.class), "friendIds")));
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MutualFriendsHello.class),
              MutualFriendsHelloRequest.builder().userId("user_id_1").build());
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
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo)
      throws Exception {
    graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.mutualFriendsHello")
            .build();
    graph.registerInputBatchers(
        ofVajram(TestUserService.class),
        InputBatcherConfig.simple(() -> new InputBatcherImpl<>(100)));
    graph.registerInputBatchers(
        ofVajram(FriendsService.class),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(FriendsService.class) + "_1",
            graph.computeDependantChain(
                getVajramIdString(MutualFriendsHello.class), "hellos", "friendIds")),
        InputBatcherConfig.sharedBatcher(
            () -> new InputBatcherImpl<>(100),
            getVajramIdString(FriendsService.class) + "_2",
            graph.computeDependantChain(getVajramIdString(MutualFriendsHello.class), "friendIds")));

    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor krystexVajramExecutor =
        graph.createExecutor(
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MutualFriendsHello.class),
              MutualFriendsHelloRequest.builder().userId("user_id_1").skip(true).build());
    }
    assertThat(multiHellos).succeedsWithin(1, TimeUnit.SECONDS);
    assertTrue(multiHellos.get().isEmpty());
    assertThat(FriendsService.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @ParameterizedTest
  @MethodSource("executorConfigsToTest")
  void flush_skippingADependency_flushesCompleteCallGraph(
      KryonExecStrategy kryonExecStrategy,
      GraphTraversalStrategy graphTraversalStrategy,
      TestInfo testInfo) {
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
        ofVajram(FriendsService.class),
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
        ofVajram(TestUserService.class),
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
            getExecutorConfig(kryonExecStrategy, graphTraversalStrategy)
                .requestId(requestContext.requestId())
                .build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              ofVajram(MultiHelloFriendsV2.class),
              MultiHelloFriendsV2Request.builder()
                  .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                  .skip(true)
                  .build());
    }
    assertThat(friendServiceFlushCommand).succeedsWithin(TIMEOUT);
    assertThat(userServiceFlushCommand).succeedsWithin(TIMEOUT);
    assertThat(multiHellos).succeedsWithin(TIMEOUT).isEqualTo("");
  }

  private HelloRequest helloRequest(TestRequestContext applicationRequestContext) {
    return helloRequestBuilder(applicationRequestContext).build();
  }

  private HelloRequest.Builder helloRequestBuilder(TestRequestContext applicationRequestContext) {
    return HelloRequest.builder().name(applicationRequestContext.loggedInUserId().orElseThrow());
  }

  private HelloRequest incompleteHelloRequest() {
    return HelloRequest.builder().build();
  }

  private TestUserServiceRequest testUserServiceRequest(TestRequestContext testRequestContext) {
    return TestUserServiceRequest.builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        .build();
  }

  private HelloFriendsRequest helloFriendsRequest(TestRequestContext testRequestContext) {
    return HelloFriendsRequest.builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        .numberOfFriends(testRequestContext.numberOfFriends())
        .build();
  }

  private HelloFriendsV2Request helloFriendsV2Request(TestRequestContext testRequestContext) {
    return HelloFriendsV2Request.builder()
        .userId(testRequestContext.loggedInUserId().orElse(null))
        .build();
  }

  private static VajramKryonGraph.Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramKryonGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);

    Predicate<LogicExecutionContext> isIOVajram =
        (context) -> {
          return getNamedValueTag(VajramTags.VAJRAM_TYPE, context.logicTags())
              .map(namedValueTag -> VajramTypes.IO_VAJRAM.equals(namedValueTag.value()))
              .orElse(false);
        };
    Function<LogicExecutionContext, String> instanceIdCreator =
        context -> {
          ImmutableMap<Object, Tag> logicTags = context.logicTags();
          Optional<Service> service = getAnnotationByType(Service.class, logicTags);
          String instanceId;
          if (service.isEmpty()) {
            Optional<NamedValueTag> namedValueTag =
                getNamedValueTag(VajramTags.VAJRAM_ID, logicTags);
            if (namedValueTag.isEmpty()) {
              throw new IllegalStateException("Missing vajramId tag");
            }
            instanceId = namedValueTag.get().value();
          } else {
            String serviceApi =
                getAnnotationByType(ServiceApi.class, logicTags)
                    .map(ServiceApi::service)
                    .map(s -> "." + s)
                    .orElse("");
            instanceId = service.get().value() + serviceApi;
          }
          return instanceId;
        };
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
    return Stream.of(
        Arguments.of(BATCH, DEPTH),
        Arguments.of(BATCH, BREADTH),
        Arguments.of(GRANULAR, DEPTH),
        Arguments.of(GRANULAR, BREADTH));
  }
}
