package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.loadFromClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.modulation.Batcher;
import com.flipkart.krystal.vajramexecutor.krystex.TestRequestContext.TestRequestContextBuilder;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello.HelloVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice.TestUserServiceVajram;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
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
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
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
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice");
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
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends");
    graph.registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher(3));
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramWithModulatorMultipleRequests").build())) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(
              vajramID(HelloFriendsVajram.ID), this::helloFriendsRequest);
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
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2");
    graph.registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher(2));
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
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
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
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends");
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
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends");
    graph.registerInputModulator(vajramID(TestUserServiceVajram.ID), () -> new Batcher(2));
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
}
