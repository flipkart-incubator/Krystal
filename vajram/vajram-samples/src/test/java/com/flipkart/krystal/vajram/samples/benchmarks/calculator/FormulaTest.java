package com.flipkart.krystal.vajram.samples.benchmarks.calculator;

import static com.flipkart.krystal.vajram.VajramID.vajramID;
import static com.flipkart.krystal.vajram.samples.Util.javaMethodBenchmark;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.adder.Adder.add;
import static com.flipkart.krystal.vajram.samples.benchmarks.calculator.divider.Divider.divide;
import static com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph.loadFromClasspath;
import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.flipkart.krystal.vajram.ApplicationRequestContext;
import com.flipkart.krystal.vajram.samples.Util;
import com.flipkart.krystal.vajramexecutor.krystex.KrystexVajramExecutor;
import com.flipkart.krystal.vajramexecutor.krystex.VajramNodeGraph;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormulaTest {

  public static final int LOOP_COUNT = 50_000;
  private VajramNodeGraph graph;

  @BeforeEach
  void setUp() {
    graph = loadFromClasspath("com.flipkart.krystal.vajram.samples.benchmarks.calculator");
  }

  @Test
  void vajram_benchmark() throws ExecutionException, InterruptedException, TimeoutException {
    long javaNativeTime = javaMethodBenchmark(FormulaTest::syncFormula, LOOP_COUNT);
    long javaFuturesTime = Util.javaFuturesBenchmark(FormulaTest::asyncFormula, LOOP_COUNT);
    try (KrystexVajramExecutor<FormulaRequestContext> krystexVajramExecutor =
        graph.createExecutor(new FormulaRequestContext(100, 20, 5, "formulaTest"))) {
      CompletableFuture<Integer>[] futures = new CompletableFuture[LOOP_COUNT];
      long startTime = System.nanoTime();
      for (int value = 0; value < LOOP_COUNT; value++) {
        CompletableFuture<Integer> result =
            krystexVajramExecutor.execute(
                vajramID(Formula.ID),
                rc -> FormulaRequest.builder().a(LOOP_COUNT).p(rc.p).q(rc.q).build(),
                "formulaTest" + value);
        futures[value] = result;
      }
      allOf(futures).get(5, TimeUnit.HOURS);
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
    }
  }

  private static void syncFormula(Integer value) {
    //noinspection ResultOfMethodCallIgnored
    divide(value, add(20, 5));
  }

  private static CompletableFuture<Integer> asyncFormula(int value) {
    CompletableFuture<Integer> numerator = completedFuture(value);
    CompletableFuture<Integer> add1 = completedFuture(20);
    CompletableFuture<Integer> add2 = completedFuture(5);
    CompletableFuture<Integer> sum =
        allOf(add1, add2).thenApply(unused -> add(add1.getNow(null), add2.getNow(null)));
    return allOf(numerator, sum)
        .thenApply(unused -> divide(numerator.getNow(null), sum.getNow(null)));
  }

  private record FormulaRequestContext(int a, int p, int q, String requestId)
      implements ApplicationRequestContext {}
}
