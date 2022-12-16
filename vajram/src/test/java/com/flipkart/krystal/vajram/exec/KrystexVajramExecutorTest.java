package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.VajramGraph.loadFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends.HelloFriendsRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends.HelloFriendsVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriendsv2.HelloFriendsV2Request;
import com.flipkart.krystal.vajram.exec.test_vajrams.hellofriendsv2.HelloFriendsV2Vajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajram;
import com.flipkart.krystal.vajram.modulation.Batcher;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KrystexVajramExecutorTest {

  private TestRequestContext requestContext;

  @BeforeEach
  void setUp() {
    requestContext =
        TestRequestContext.builder()
            .loggedInUserId(Optional.of("user_id_1"))
            .numberOfFriends(2)
            .build();
  }

  @AfterEach
  void tearDown() {
    TestUserServiceVajram.CALL_COUNTER.set(0);
  }

  @Test
  void requestExecution_vajramWithNoDependencies_success() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams.hello");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        new KrystexVajramExecutor<>(
            vajramGraph, "requestExecution_vajramWithNoDependencies_success", requestContext)) {
      CompletableFuture<String> result =
          krystexVajramExecutor.execute(new VajramID(HelloVajram.ID), this::helloRequest);
      assertEquals("Hello! user_id_1", result.get(5, TimeUnit.HOURS));
    }
  }

  @Test
  void requestExecution_ioVajramSingleRequestNoModulator_success() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams.userservice");
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        new KrystexVajramExecutor<>(vajramGraph, "", requestContext)) {
      CompletableFuture<TestUserInfo> userInfo123 =
          krystexVajramExecutor.execute(
              new VajramID(TestUserServiceVajram.ID), this::testUserServiceRequest);
      assertEquals("Firstname Lastname (user_id_1)", userInfo123.get(5, TimeUnit.HOURS).userName());
    }
  }

  @Test
  void requestExecution_ioVajramWithModulatorMultipleRequests_calledOnlyOnce() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath(
            "com.flipkart.krystal.vajram.exec.test_vajrams.userservice",
            "com.flipkart.krystal.vajram.exec.test_vajrams.hellofriends");
    vajramGraph.registerInputModulator(
        new VajramID(TestUserServiceVajram.ID), () -> new Batcher(3));
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        new KrystexVajramExecutor<>(
            vajramGraph,
            "requestExecution_ioVajramWithModulatorMultipleRequests_calledOnlyOnce",
            requestContext)) {
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
  void requestExecution_sequentialDependency_success() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath(
            "com.flipkart.krystal.vajram.exec.test_vajrams.userservice",
            "com.flipkart.krystal.vajram.exec.test_vajrams.friendsservice",
            "com.flipkart.krystal.vajram.exec.test_vajrams.hellofriendsv2");
    vajramGraph.registerInputModulator(
        new VajramID(TestUserServiceVajram.ID), () -> new Batcher(2));
    try (KrystexVajramExecutor<TestRequestContext> krystexVajramExecutor =
        new KrystexVajramExecutor<>(
            vajramGraph, "requestExecution_sequentialDependency_success", requestContext)) {
      CompletableFuture<String> helloString =
          krystexVajramExecutor.execute(
              new VajramID(HelloFriendsV2Vajram.ID), this::helloFriendsV2Request);
      assertEquals(
          "Hello Friends! Firstname Lastname (user_id_1:friend1), Firstname Lastname (user_id_1:friend2)",
          helloString.get(5, TimeUnit.HOURS));
      assertEquals(1, TestUserServiceVajram.CALL_COUNTER.get());
    }
  }

  private HelloRequest helloRequest(TestRequestContext applicationRequestContext) {
    return HelloRequest.builder()
        .name(applicationRequestContext.loggedInUserId().orElse(null))
        .build();
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
