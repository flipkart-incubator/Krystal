package com.flipkart.krystal.vajram.samples;

import static java.util.concurrent.CompletableFuture.allOf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

public class Util {

  public static long javaMethodBenchmark(Consumer<Integer> consumer, int loopCount) {
    long startTime = System.nanoTime();
    for (int value = 0; value < loopCount; value++) {
      consumer.accept(value);
    }
    long time = System.nanoTime() - startTime;
    System.out.printf("Total java method time: %,d \n", time);
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
    System.out.printf("Total java futures time: %,d \n", time);
    return time;
  }
}
