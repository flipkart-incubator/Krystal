package com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.Util.javaFuturesBenchmark;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.loadFromClasspath;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChainAdderTest {
  public static final int LOOP_COUNT = 1000;
  private VajramNodeGraph graph;

  @BeforeEach
  void setUp() throws ExecutionException, InterruptedException, TimeoutException {
    graph = loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator");
  }

//  @Test
  void vajram_benchmark()
      throws ExecutionException, InterruptedException, TimeoutException {
    try (KrystexVajramExecutor<RequestContext> krystexVajramExecutor =
        graph.createExecutor(new RequestContext(""))) {
      long javaNativeTime = javaMethodBenchmark(this::chainAdd, LOOP_COUNT);
      long javaFuturesTime = javaFuturesBenchmark(this::chainAddAsync, LOOP_COUNT);
      CompletableFuture<Integer>[] futures = new CompletableFuture[LOOP_COUNT];
      krystexVajramExecutor.execute(
          vajramID(ChainAdder.ID),
          rc ->
              ChainAdderRequest.builder()
                  .numbers(new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
                  .build(),
          "chainAdderTest");
      long startTime = System.nanoTime();
      for (int value = 0; value < LOOP_COUNT; value++) {
        int localValue = value;
        CompletableFuture<Integer> result =
            krystexVajramExecutor.execute(
                vajramID(ChainAdder.ID),
                rc ->
                    ChainAdderRequest.builder()
                        .numbers(
                            new ArrayList<>(
                                Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                                    .map(integer -> integer + localValue * 10)
                                    .toList()))
                        .build(),
                "chainAdderTest" + localValue);
        futures[value] = result;
      }
      assertThat(allOf(futures).thenCompose(unused -> futures[0]).get(5, TimeUnit.HOURS))
          .isEqualTo(55);
      long vajramTime = System.nanoTime() - startTime;
      System.out.printf("vajram: %,d ns for %,d requests", vajramTime, LOOP_COUNT);
      System.out.println();
      System.out.printf(
          "Platform overhead over native code: %,.0f ns per request",
          (1.0 * vajramTime - javaNativeTime) / LOOP_COUNT);
      System.out.println();
      System.out.printf(
          "Platform overhead over reactive code: %,.0f ns per request",
          (1.0 * vajramTime - javaFuturesTime) / LOOP_COUNT);
      System.out.println();
    }
  }

  private void chainAdd(Integer value) {
    chainAdd(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private int chainAdd(List<Integer> numbers) {
    if (numbers.size() == 0) {
      return 0;
    } else if (numbers.size() == 1) {
      return add(numbers.get(0), 0);
    } else if (numbers.size() == 2) {
      return add(numbers.get(0), numbers.get(1));
    } else {
      return chainAdd(numbers.subList(0, numbers.size() - 1))
          + add(numbers.get(numbers.size() - 1), 0);
    }
  }

  private CompletableFuture<Integer> chainAddAsync(Integer value) {
    return chainAddAsync(
        new ArrayList<>(
            Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .map(integer -> integer + value * 10)
                .toList()));
  }

  private CompletableFuture<Integer> chainAddAsync(List<Integer> numbers) {
    if (numbers.size() == 0) {
      return completedFuture(0);
    } else if (numbers.size() == 1) {
      return addAsync(numbers.get(0), 0);
    } else if (numbers.size() == 2) {
      return addAsync(numbers.get(0), numbers.get(1));
    } else {
      return chainAddAsync(numbers.subList(0, numbers.size() - 1))
          .thenCombine(addAsync(numbers.get(numbers.size() - 1), 0), Integer::sum);
    }
  }

  private CompletableFuture<Integer> addAsync(int a, int b) {
    return completedFuture(a + b);
  }

  record RequestContext(String requestId) implements ApplicationRequestContext {}
}
