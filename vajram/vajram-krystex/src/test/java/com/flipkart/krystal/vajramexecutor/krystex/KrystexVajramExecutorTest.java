package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.krystex.decoration.LogicDecorationOrdering;
import com.flipkart.krystal.krystex.decoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.decoration.MainLogicDecoratorConfig;
import com.flipkart.krystal.krystex.decorators.Resilience4JBulkhead;
import com.flipkart.krystal.krystex.decorators.Resilience4JCircuitBreaker;
import com.flipkart.krystal.logic.LogicTag;
import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajram.tags.Service;
import com.flipkart.krystal.vajram.tags.ServiceApi;
import com.flipkart.krystal.vajram.tags.VajramTags;
import com.flipkart.krystal.vajram.tags.VajramTags.VajramTypes;
import com.flipkart.krystal.vajramexecutor.krystex.TestRequestContext.TestRequestContextBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.Builder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriends;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello.MultiHelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.junit.jupiter.api.Test;

class KrystexVajramExecutorTest {

  private TestRequestContextBuilder requestContext;

  @BeforeEach
  void setUp() {
    requestContext =
        TestRequestContext.builder().loggedInUserId(Optional.of("user_id_1")).numberOfFriends(2);
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
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(vajramID(HelloVajram.ID), this::helloRequest);
      assertEquals("Hello! user_id_1", result.get(5, TimeUnit.HOURS));
    }
  }

  @Test
  void executeCompute_optionalInputProvided_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(
              vajramID(HelloVajram.ID),
              applicationRequestContext ->
                  helloRequestBuilder(applicationRequestContext).greeting("Namaste").build());
      assertEquals("Namaste! user_id_1", result.get(5, TimeUnit.HOURS));
    }
  }

  @Test
  void executeIo_singleRequestNoModulator_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice")
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramSingleRequestNoModulator").build())) {
      CompletableFuture<TestUserInfo> userInfo123 =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID), this::testUserServiceRequest);
      assertEquals("Firstname Lastname (user_id_1)", userInfo123.get(5, TimeUnit.HOURS).userName());
    }
  }

  @Test
  void executeIo_withModulatorMultipleRequests_calledOnlyOnce() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher<>(3))
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramWithModulatorMultipleRequests").build())) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(vajramID(HelloFriendsVajram.ID), this::helloFriendsRequest);
      assertEquals(
          "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)",
          helloString.get(5, TimeUnit.HOURS));
      assertEquals(1, TestUserServiceVajram.CALL_COUNTER.sum());
    }
  }

  @Test
  void executeCompute_sequentialDependency_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2")
            .registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher<>(2))
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("sequentialDependency").build())) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsV2Vajram.ID), this::helloFriendsV2Request);
      assertEquals(
          "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)",
          helloString.get(5, TimeUnit.HOURS));
      assertEquals(1, TestUserServiceVajram.CALL_COUNTER.sum());
    }
  }

  @Test
  void executeCompute_missingMandatoryInput_throwsException() {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello").build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(vajramID(HelloVajram.ID), this::incompleteHelloRequest);
      assertThatThrownBy(() -> result.get(5, TimeUnit.HOURS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseExactlyInstanceOf(MandatoryInputsMissingException.class)
          .hasMessageContaining(
              "Vajram v<" + HelloVajram.ID + "> did not receive these mandatory inputs: [ name");
    }
  }

  @Test
  void execute_multiRequestNoInputModulator_cacheHitSuccess() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("multiRequestNoInputModulator_cacheHitSuccess").build())) {
      CompletableFuture<TestUserInfo> userInfo =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID),
              testRequestContext -> TestUserServiceRequest.builder().userId("user_id_1").build());
      CompletableFuture<String> helloFriends =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsVajram.ID),
              testRequestContext ->
                  HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(0).build());
      assertThat(userInfo.get(5, TimeUnit.HOURS).userName())
          .isEqualTo("Firstname Lastname (user_id_1)");
      assertThat(helloFriends.get(5, TimeUnit.HOURS))
          .isEqualTo("Hello Friends of Firstname Lastname (user_id_1)! ");
      assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(1);
    }
  }

  @Test
  void execute_multiRequestWithModulator_cacheHitSuccess() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends")
            .registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher<>(2))
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramSingleRequestNoModulator").build())) {
      CompletableFuture<TestUserInfo> userInfo =
          krystexVajramExecutor.execute(
              vajramID(TestUserServiceVajram.ID),
              testRequestContext ->
                  TestUserServiceRequest.builder().userId("user_id_1:friend_1").build());
      CompletableFuture<String> helloFriends =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsVajram.ID),
              testRequestContext ->
                  HelloFriendsRequest.builder().userId("user_id_1").numberOfFriends(1).build());
      assertThat(userInfo.get(5, TimeUnit.HOURS).userName())
          .isEqualTo("Firstname Lastname (user_id_1:friend_1)");
      assertThat(helloFriends.get(5, TimeUnit.HOURS))
          .isEqualTo(
              "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)");
      assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(1);
      assertThat(TestUserServiceVajram.REQUESTS)
          .isEqualTo(
              Set.of(
                  TestUserServiceRequest.builder().userId("user_id_1:friend_1").build(),
                  TestUserServiceRequest.builder().userId("user_id_1").build()));
    }
  }

  @Test
  void execute_multiResolverFanouts_permutesTheFanouts()
      throws ExecutionException, InterruptedException {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher<>(6))
            .build();
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("execute_multiResolverFanouts_permutesTheFanouts").build())) {
      CompletableFuture<String> multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriends.ID),
              testRequestContext ->
                  MultiHelloFriendsRequest.builder()
                      .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                      .build());
      assertThat(multiHellos.get())
          .isEqualTo(
              """
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    }
  }

  @Test
  void close_causesBatcherToTerminate() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends",
                "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.multihello")
            .registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher<>(100))
            .build();
    CompletableFuture<String> multiHellos;
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("execute_multiResolverFanouts_permutesTheFanouts").build())) {
      multiHellos =
          krystexVajramExecutor.execute(
              vajramID(MultiHelloFriends.ID),
              testRequestContext ->
                  MultiHelloFriendsRequest.builder()
                      .userIds(new ArrayList<>(List.of("user_id_1", "user_id_2")))
                      .build());
    }
    assertThat(multiHellos.get())
        .isEqualTo(
            """
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1)
            Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1)
            Hello Friends of Firstname Lastname (user_id_2)! Firstname Lastname (user_id_2:friend_1), Firstname Lastname (user_id_2:friend_2)""");
    assertThat(TestUserServiceVajram.CALL_COUNTER.sum()).isEqualTo(1);
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
    Function<LogicExecutionContext, String> createInstanceId =
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
    builder.decorateVajramLogicForSession(
        new MainLogicDecoratorConfig(
            Resilience4JBulkhead.DECORATOR_TYPE,
            isIOVajram,
            createInstanceId,
            Resilience4JBulkhead::new));
    builder.decorateVajramLogicForSession(
        new MainLogicDecoratorConfig(
            Resilience4JCircuitBreaker.DECORATOR_TYPE,
            isIOVajram,
            createInstanceId,
            Resilience4JCircuitBreaker::new));
    builder.logicDecorationOrdering(
        new LogicDecorationOrdering(
            ImmutableSet.of(
                Resilience4JCircuitBreaker.DECORATOR_TYPE,
                Resilience4JBulkhead.DECORATOR_TYPE,
                InputModulationDecorator.DECORATOR_TYPE)));
    return builder;
  }
}
