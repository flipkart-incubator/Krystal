package com.flipkart.krystal.vajram.exec;

import static com.flipkart.krystal.vajram.exec.VajramGraph.loadFromClasspath;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloRequest;
import com.flipkart.krystal.vajram.exec.test_vajrams.hello.HelloVajram;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;

class KrystexVajramExecutorTest {

  @Test
  void requestExecution_vajramWithNoDependencies_success()
      throws ExecutionException, InterruptedException, TimeoutException {
    VajramGraph vajramGraph =
        loadFromClasspath("com.flipkart.krystal.vajram.exec.test_vajrams.hello");
    try (KrystexVajramExecutor krystexVajramExecutor =
        new KrystexVajramExecutor(vajramGraph, "qwerty")) {
      CompletableFuture<String> result =
          krystexVajramExecutor.requestExecution(HelloVajram.ID, new HelloRequest("Suma"));
      assertEquals("Hello! Suma", result.get(5, TimeUnit.HOURS));
    }
  }
}
