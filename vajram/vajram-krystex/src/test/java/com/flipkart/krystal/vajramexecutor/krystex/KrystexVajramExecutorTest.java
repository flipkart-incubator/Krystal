package com.flipkart.krystal.vajramexecutor.krystex;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.flipkart.krystal.krystex.node.DependantChain.fromTriggerOrder;
import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flipkart.krystal.krystex.MainLogic;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecorator;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decorators.resilience4j.Resilience4JBulkhead;
import com.flipkart.krystal.krystex.decorators.resilience4j.Resilience4JCircuitBreaker;
import com.flipkart.krystal.krystex.node.NodeId;
import com.flipkart.krystal.krystex.node.ObservabilityConfig;
import com.flipkart.krystal.krystex.node.ObservationData;
import com.flipkart.krystal.logic.LogicTag;
import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajram.tags.Service;
import com.flipkart.krystal.vajram.tags.ServiceApi;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.flipkart.krystal.vajram.tags.VajramTags.VajramTypes;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice.FriendsServiceVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2.MultiHelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class KrystexVajramExecutorTest {

  private TestRequestContext requestContext;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
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
    TestUserServiceVajram.CALL_COUNTER.reset();
    TestUserServiceVajram.REQUESTS.clear();
    HelloVajram.CALL_COUNTER.reset();
  }

  @Test
  void executeCompute_noDependencies_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      result = krystexVajramExecutor.execute(vajramID(HelloVajram.ID), this::helloRequest);
    }
    assertEquals("Hello! user_id_1", timedGet(result));
  }

  @Test
  void executeCompute_optionalInputProvided_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      result =
          krystexVajramExecutor.execute(
              vajramID(HelloVajram.ID),
              applicationRequestContext ->
                  helloRequestBuilder(applicationRequestContext).greeting("Namaste").build());
    }
    assertEquals("Namaste! user_id_1", timedGet(result));
  }

  @Test
  void executeIo_singleRequestNoModulator_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice")
            .build();
    CompletableFuture<TestUserInfo> userInfo123;
    requestContext.requestId("ioVajramSingleRequestNoModulator");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      userInfo123 =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID), this::testUserServiceRequest);
    }
    assertEquals("Firstname Lastname (user_id_1)", timedGet(userInfo123).userName());
  }

  @Test
  void executeIo_withModulatorMultipleRequests_calledOnlyOnce() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.sharedModulator(
                    () -> new Batcher<>(3),
                    TestUserServiceVajram.ID + "Batcher",
                    ImmutableSet.of(
                        fromTriggerOrder(new NodeId(HelloFriendsVajram.ID), "user_infos"),
                        fromTriggerOrder(new NodeId(HelloFriendsVajram.ID), "friend_infos"))))
            .build();
    CompletableFuture<String> helloString;
    requestContext.requestId("ioVajramWithModulatorMultipleRequests");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      helloString =
          krystexVajramExecutor.execute(vajramID(HelloFriendsVajram.ID), this::helloFriendsRequest);
    }
    assertEquals(
        "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)",
        timedGet(helloString));
    assertEquals(1, TestUserServiceVajram.CALL_COUNTER.sum());
  }

  @Test
  void executeCompute_sequentialDependency_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.simple(() -> new Batcher<>(2)))
            .build();
    CompletableFuture<String> helloString;
    requestContext.requestId("sequentialDependency");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      helloString =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsV2Vajram.ID), this::helloFriendsV2Request);
    }
    assertEquals(
        "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)",
        timedGet(helloString));
    assertEquals(1, TestUserServiceVajram.CALL_COUNTER.sum());
  }

  @Test
  void executeCompute_missingMandatoryInput_throwsException() {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    CompletableFuture<String> result;
    requestContext.requestId("vajramWithNoDependencies");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      result =
          krystexVajramExecutor.execute(vajramID(HelloVajram.ID), this::incompleteHelloRequest);
    }
    assertThatThrownBy(() -> timedGet(result))
        .isInstanceOf(ExecutionException.class)
        .hasCauseExactlyInstanceOf(MandatoryInputsMissingException.class)
        .hasMessageContaining(
            "Vajram v<" + HelloVajram.ID + "> did not receive these mandatory inputs: [ name");
  }

  @Test
  void execute_multiRequestNoInputModulator_cacheHitSuccess() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    CompletableFuture<TestUserInfo> userInfo;
    CompletableFuture<String> helloFriends;
    requestContext.requestId("multiRequestNoInputModulator_cacheHitSuccess");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      userInfo =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID),
              testRequestContext -> TestUserServiceRequest.builder().userId("user_id_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsVajram.ID),
              testRequestContext ->
                  HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(0).build());
    }
    assertThat(timedGet(userInfo).userName()).isEqualTo("Firstname Lastname (user_id_1)");
    assertThat(timedGet(helloFriends))
        .isEqualTo("Hello Friends of Firstname Lastname (user_id_1)! ");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  void execute_multiRequestWithModulator_cacheHitSuccess() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    CompletableFuture<TestUserInfo> userInfo;
    CompletableFuture<String> helloFriends;
    requestContext.requestId("ioVajramSingleRequestNoModulator");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      userInfo =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID),
              testRequestContext ->
                  TestUserServiceRequest.builder().userId("user_id_1:friend_1").build());
      helloFriends =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsVajram.ID),
              testRequestContext ->
                  HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(1).build());
    }
    assertThat(timedGet(userInfo).userName()).isEqualTo("Firstname Lastname (user_id_1:friend_1)");
    assertThat(timedGet(helloFriends))
        .isEqualTo(
            "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(2);
    assertThat(TestUserServiceVajram.REQUESTS)
        .isEqualTo(
            Set.of(
                TestUserServiceRequest.builder().userId("user_id_1:friend_1").build(),
                TestUserServiceRequest.builder().userId("user_id_1").build()));
  }

  @Test
  void execute_multiResolverFanouts_permutesTheFanouts() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.simple(() -> new Batcher<>(6)))
            .build();
    CompletableFuture<String> multiHellos;
    requestContext.requestId("execute_multiResolverFanouts_permutesTheFanouts");
    ObservationData nodeExecutionReport;
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext, new ObservabilityConfig(true, true))) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriends.ID),
              testRequestContext ->
                  MultiHelloFriendsRequest.builder()
                      .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                      .build());
      nodeExecutionReport = krystexVajramExecutor.getKrystalExecutor().getObservationData();
    }
    assertThat(timedGet(multiHellos))
        .isEqualTo(
            """
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    System.out.println(
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(nodeExecutionReport));
  }

  @Test
  void flush_singleDepthParallelDependencyDefaultInputModulatorConfig_flushes2Batchers(
      TestInfo testInfo) throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.simple(() -> new Batcher<>(100)))
            .build();
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriends.ID),
              testRequestContext ->
                  MultiHelloFriendsRequest.builder()
                      .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                      .build());
    }
    assertThat(timedGet(multiHellos))
        .isEqualTo(
            """
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum())
        .isEqualTo(
            /*
             Default InputModulatorConfig allocates one InputModulationDecorator for each
             dependant call chain.
             TestUserServiceVajram is called via two dependantChains:
             [Start]>MultiHelloFriends:hellos>HelloFriendsVajram:user_infos
             [Start]>MultiHelloFriends:hellos>HelloFriendsVajram:friend_infos
            */
            2);
  }

  @Test
  void flush_singleDepthParallelDependencySharedInputModulatorConfig_flushes1Batcher(
      TestInfo testInfo) throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.sharedModulator(
                    () -> new Batcher<>(100),
                    TestUserServiceVajram.ID + "Batcher",
                    ImmutableSet.of(
                        fromTriggerOrder(new NodeId(MultiHelloFriends.ID), "hellos", "user_infos"),
                        fromTriggerOrder(
                            new NodeId(MultiHelloFriends.ID), "hellos", "friend_infos"))))
            .build();
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriends.ID),
              testRequestContext ->
                  MultiHelloFriendsRequest.builder()
                      .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                      .build());
    }
    assertThat(timedGet(multiHellos))
        .isEqualTo(
            """
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
              Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
              Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum())
        .isEqualTo(
            /*
             TestUserServiceVajram is called via two dependantChains:
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:user_infos)
             ([Start]>MultiHelloFriends:hellos>HelloFriendsVajram:friend_infos)
             Since input modulator is shared across these dependantChains, only one call must be
             made
            */
            1);
  }

  @Test
  void close_sequentialDependency_flushesBatcher(TestInfo testInfo) throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2")
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                InputModulatorConfig.simple(() -> new Batcher<>(100)))
            .registerInputModulator(
                vajramID(FriendsServiceVajram.ID),
                InputModulatorConfig.simple(() -> new Batcher<>(100)))
            .build();
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriendsV2.ID),
              testRequestContext ->
                  MultiHelloFriendsV2Request.builder()
                      .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                      .build());
    }
    assertThat(timedGet(multiHellos))
        .isEqualTo(
            """
            Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)
            Hello Friends! Firstname Lastname (user_id_2:friend1), Firstname Lastname (user_id_2:friend2)""");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(1);
  }

  @Test
  @Disabled("Fix: https://github.com/flipkart-incubator/Krystal/issues/84")
  void flush_skippingADependency_flushesCompleteCallGraph(TestInfo testInfo) throws Exception {
    CompletableFuture<FlushCommand> friendServiceFlushCommand = new CompletableFuture<>();
    CompletableFuture<FlushCommand> userServiceFlushCommand = new CompletableFuture<>();
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihellov2")
            .registerInputModulator(
                vajramID(FriendsServiceVajram.ID),
                new InputModulatorConfig(
                    logicExecutionContext -> "",
                    modulatorContext ->
                        new MainLogicDecorator() {
                          @Override
                          public MainLogic<Object> decorateLogic(MainLogic<Object> mainLogic) {
                            return mainLogic;
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
                        }))
            .registerInputModulator(
                vajramID(TestUserServiceVajram.ID),
                new InputModulatorConfig(
                    logicExecutionContext1 -> "",
                    modulatorContext1 ->
                        new MainLogicDecorator() {
                          @Override
                          public MainLogic<Object> decorateLogic(MainLogic<Object> mainLogic) {
                            return mainLogic;
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
                        }))
            .build();
    CompletableFuture<String> multiHellos;
    requestContext.requestId(testInfo.getDisplayName());
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext)) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriendsV2.ID),
              testRequestContext ->
                  MultiHelloFriendsV2Request.builder()
                      .userIds(new LinkedHashSet<>(List.of("user_id_1", "user_id_2")))
                      .skip(true)
                      .build());
    }
    assertThat(friendServiceFlushCommand).succeedsWithin(ofSeconds(1));
    assertThat(userServiceFlushCommand).succeedsWithin(ofSeconds(1));
    assertThat(timedGet(multiHellos)).isEqualTo("");
  }

  private HelloRequest helloRequest(TestRequestContext applicationRequestContext) {
    return helloRequestBuilder(applicationRequestContext).build();
  }

  private HelloRequest.Builder helloRequestBuilder(TestRequestContext applicationRequestContext) {
    return HelloRequest.builder().name(applicationRequestContext.loggedInUserId().orElseThrow());
  }

  private HelloRequest incompleteHelloRequest(TestRequestContext applicationRequestContext) {
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

  private static VajramNodeGraph.Builder loadFromClasspath(String... packagePrefixes) {
    Builder builder = VajramNodeGraph.builder();
    Arrays.stream(packagePrefixes).forEach(builder::loadFromPackage);
    Predicate<LogicExecutionContext> isIOVajram =
        context ->
            Optional.ofNullable(context.logicTags().get(VajramTags.VAJRAM_TYPE))
                .map(LogicTag::tagValue)
                .map(VajramTypes.IO_VAJRAM::equals)
                .orElse(false);
    Function<LogicExecutionContext, String> instanceIdCreator =
        context -> {
          ImmutableMap<String, LogicTag> logicTags = context.logicTags();
          LogicTag service = logicTags.get(Service.TAG_KEY);
          String instanceId;
          if (service == null) {
            LogicTag vajramId = logicTags.get(VajramTags.VAJRAM_ID);
            if (vajramId == null) {
              throw new IllegalStateException();
            }
            instanceId = vajramId.tagValue();
          } else {
            String serviceApi =
                Optional.ofNullable(logicTags.get(ServiceApi.TAG_KEY)).map(s -> "." + s).orElse("");
            instanceId = service.tagValue() + serviceApi;
          }
          return instanceId;
        };
    return builder
        .decorateVajramLogicForSession(
            new MainLogicDecoratorConfig(
                Resilience4JBulkhead.DECORATOR_TYPE,
                isIOVajram,
                instanceIdCreator,
                decoratorContext ->
                    new Resilience4JBulkhead(
                        instanceIdCreator.apply(decoratorContext.logicExecutionContext()))))
        .decorateVajramLogicForSession(
            new MainLogicDecoratorConfig(
                Resilience4JCircuitBreaker.DECORATOR_TYPE,
                isIOVajram,
                instanceIdCreator,
                decoratorContext ->
                    new Resilience4JCircuitBreaker(
                        instanceIdCreator.apply(decoratorContext.logicExecutionContext()))))
        .logicDecorationOrdering(
            new LogicDecorationOrdering(
                ImmutableSet.of(
                    Resilience4JCircuitBreaker.DECORATOR_TYPE,
                    Resilience4JBulkhead.DECORATOR_TYPE,
                    InputModulationDecorator.DECORATOR_TYPE)));
  }

  /* So that bad testcases do not hang indefinitely.*/
  private static <T> T timedGet(CompletableFuture<T> future) throws Exception {
    return future.get(1, TimeUnit.HOURS);
  }
}
