package com.flipkart.krystal.utils;

import java.util.concurrent.CompletableFuture;

public class Futures {

  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    from.whenComplete(
        (unused, throwable) -> {
          if (from.isCancelled() && !to.isDone()) {
            to.cancel(true);
          }
        });
  }

  public static <T>  void propagateCompletion(CompletableFuture<? extends T> from, CompletableFuture<T> to) {
    from.whenComplete(
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(error);
          } else {
            to.complete(result);
          }
        });
  }

  private Futures() {}
}
