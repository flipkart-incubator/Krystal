package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.VajramGraph.loadFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.vajram.VajramID;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserInfo;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajram;
import com.flipkart.krystal.vajram.exec.test_vajrams.userservice.TestUserServiceVajramRequest;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class KrystexVajramExecutorTest {

  @Test
  void requestExecution_vajramWithNoDependencies_success() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams.hello");
    try (KrystexVajramExecutor krystexVajramExecutor =
        new KrystexVajramExecutor(
            vajramGraph, "requestExecution_vajramWithNoDependencies_success")) {
      CompletableFuture<String> result =
          krystexVajramExecutor.requestExecution(
              new VajramID(HelloVajram.ID), new HelloRequest("Suma"));
      assertEquals("Hello! Suma", result.get(5, TimeUnit.SECONDS));
    }
  }

  @Test
  void requestExecution_ioVajramSingleRequest_success() throws Exception {
    VajramGraph vajramGraph =
        loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams.userservice");
    try (KrystexVajramExecutor krystexVajramExecutor =
        new KrystexVajramExecutor(vajramGraph, "requestExecution_ioVajramSimpleBatching_success")) {
      CompletableFuture<?> userInfo123 =
          krystexVajramExecutor.requestExecution(
              new VajramID(TestUserServiceVajram.ID), new TestUserServiceVajramRequest("user123"));
      assertEquals(
          new TestUserInfo("Firstname Lastname (user123)"), userInfo123.get(5, TimeUnit.HOURS));
    }
  }
}
