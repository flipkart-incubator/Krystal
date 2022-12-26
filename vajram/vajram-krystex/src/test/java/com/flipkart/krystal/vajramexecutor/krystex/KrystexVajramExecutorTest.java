package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.loadFromClasspath;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.vajram.MandatoryInputsMissingException;
import com.flipkart.krystal.vajram.VajramID;
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
    TestUserServiceVajram.CALL_COUNTER.set(0);
  }

  @Test
  void execute_computeNoDependencies_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(new VajramID(HelloVajram.ID), this::helloRequest);
      assertEquals("Hello! user_id_1", result.get(5, TimeUnit.HOURS));
    }
  }

  @Test
  void execute_computeOptionalInputProvided_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(
              new VajramID(HelloVajram.ID),
              applicationRequestContext ->
                  helloRequestBuilder(applicationRequestContext).greeting("Namaste").build());
      assertEquals("Namaste! user_id_1", result.get(5, TimeUnit.HOURS));
    }
  }

  @Test
  void execute_ioSingleRequestNoModulator_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramSingleRequestNoModulator").build())) {
      CompletableFuture<TestUserInfo> userInfo123 =
          krystexVajramExecutor.execute(
              new VajramID(TestUserServiceVajram.ID), this::testUserServiceRequest);
      assertEquals("Firstname Lastname (user_id_1)", userInfo123.get(5, TimeUnit.HOURS).userName());
    }
  }

  @Test
  void execute_ioVajramWithModulatorMultipleRequests_calledOnlyOnce() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriends");
    graph.registerInputModulator(new VajramID(TestUserServiceVajram.ID), () -> new Batcher(3));
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(
            requestContext.requestId("ioVajramWithModulatorMultipleRequests").build())) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(
              new VajramID(HelloFriendsVajram.ID), this::helloFriendsRequest);
      assertEquals(
          "Hello Friends of Firstname Lastname (user_id_1)! Firstname Lastname (user_id_1:friend_1), Firstname Lastname (user_id_1:friend_2)",
          helloString.get(5, TimeUnit.HOURS));
      assertEquals(1, TestUserServiceVajram.CALL_COUNTER.get());
    }
  }

  @Test
  void execute_sequentialDependency_success() throws Exception {
    VajramNodeGraph graph =
        loadFromClasspath(
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.userservice",
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.friendsservice",
            "com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hellofriendsv2");
    graph.registerInputModulator(new VajramID(TestUserServiceVajram.ID), () -> new Batcher(2));
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("sequentialDependency").build())) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(
              new VajramID(HelloFriendsV2Vajram.ID), this::helloFriendsV2Request);
      assertEquals(
          "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)",
          helloString.get(5, TimeUnit.HOURS));
      assertEquals(1, TestUserServiceVajram.CALL_COUNTER.get());
    }
  }

  @Test
  void requestExecution_missingMandatoryInput_throwsException() {
    VajramNodeGraph graph =
        loadFromClasspath("com.flipkart.krystal.vajramexecutor.krystex.test_vajrams.hello");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        graph.createExecutor(requestContext.requestId("vajramWithNoDependencies").build())) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(new VajramID(HelloVajram.ID), this::incompleteHelloRequest);
      assertThatThrownBy(() -> result.get(5, TimeUnit.HOURS))
          .isInstanceOf(ExecutionException.class)
          .hasCauseExactlyInstanceOf(MandatoryInputsMissingException.class)
          .hasMessageContaining(
              "Vajram v<" + HelloVajram.ID + "> did not receive these mandatory inputs: [ name");
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
