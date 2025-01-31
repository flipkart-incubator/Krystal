package com.flipkart.krystal.vajram.samples;

import static java.time.Duration.ofNanos;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.stream;
import static java.util.concurrent.CompletableFuture.allOf;

import com.flipkart.krystal.krystex.kryon.KryonExecutorMetrics;
import com.flipkart.krystal.pooling.MultiLeasePool;
import com.flipkart.krystal.pooling.MultiLeasePoolStats;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph;
import com.flipkart.krystal.vajramexecutor.krystex.VajramKryonGraph.VajramKryonGraphBuilder;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Util {

  public static final Duration TEST_TIMEOUT = ofSeconds(1);

  private Util() {}

  public static long javaMethodBenchmark(Consumer<Integer> consumer, int loopCount) {
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      consumer.accept(value);
    }
    long time = System.nanoTime() - startTime;
    System.out.printf("Total java method time: %,d%n", time);
    return time;
  }

  public static long javaFuturesBenchmark(
      Function<Integer, CompletableFuture<?>> computationProvider, int loopCount)
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<?>[] futures = new CompletableFuture[loopCount];
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      futures[value] = computationProvider.apply(value);
    }
    allOf(futures).get(5, TimeUnit.HOURS);
    long time = System.nanoTime() - startTime;
    System.out.printf("Total java futures time: %,d%n", time);
    return time;
  }

  public static void printStats(
      int loopCount,
      long javaNativeTimeNs,
      long javaFuturesTimeNs,
      KryonExecutorMetrics[] metrics,
      long timeToCreateExecutors,
      long timeToEnqueueVajram,
      long vajramTimeNs,
      MultiLeasePool<?> pool) {
    printStats(
        loopCount,
        1,
        javaNativeTimeNs,
        javaFuturesTimeNs,
        metrics,
        timeToCreateExecutors,
        timeToEnqueueVajram,
        vajramTimeNs,
        pool);
  }

  public static void printStats(
      int outerLoopCount,
      int innerLoopCount,
      long javaNativeTimeNs,
      long javaFuturesTimeNs,
      KryonExecutorMetrics[] metrics,
      long timeToCreateExecutors,
      long timeToEnqueueVajram,
      long vajramTimeNs,
      MultiLeasePool<?> multiLeasePool) {
    int loopCount = outerLoopCount * innerLoopCount;
    MultiLeasePoolStats multiLeasePoolStats = multiLeasePool.stats();
    System.out
        .printf("Outer Loop Count: %,d%n", outerLoopCount)
        .printf("Inner Loop Count: %,d%n", innerLoopCount)
        .printf("Avg. time to Create Executors:%,d ns%n", timeToCreateExecutors / outerLoopCount)
        .printf("Avg. time to Enqueue vajrams:%,d ns%n", timeToEnqueueVajram / loopCount)
        .printf("Avg. time to execute vajrams:%,d ns%n", vajramTimeNs / loopCount)
        .printf(
            "Throughput executions/s: %d%n",
            loopCount * Duration.ofSeconds(1).toNanos() / ofNanos(vajramTimeNs).toNanos())
        .printf(
            "CommandsQueuedCount: %,d%n",
            stream(metrics).mapToInt(KryonExecutorMetrics::commandQueuedCount).sum())
        .printf(
            "CommandQueueBypassedCount: %,d%n",
            stream(metrics).mapToInt(KryonExecutorMetrics::commandQueueBypassedCount).sum())
        .printf(
            "Platform overhead over native code: %,.0f ns per request%n",
            (1.0 * vajramTimeNs - javaNativeTimeNs) / loopCount)
        .printf(
            "Platform overhead over reactive code: %,.0f ns per request%n",
            (1.0 * vajramTimeNs - javaFuturesTimeNs) / loopCount)
        .printf(
            "maxActiveLeasesPerObject: %s, peakAvgActiveLeasesPerObject: %s, maxPoolSize: %s%n",
            multiLeasePoolStats.peakLeasesOfAnObject(),
            multiLeasePoolStats.peakAvgActiveLeasesPerObject(),
            multiLeasePoolStats.peakPoolSize());
  }

  public static VajramKryonGraphBuilder loadFromClasspath(String... packagePrefixes) {
    VajramKryonGraphBuilder builder = VajramKryonGraph.builder();
    stream(packagePrefixes).forEach(builder::loadFromPackage);
    return builder;
  }
}
