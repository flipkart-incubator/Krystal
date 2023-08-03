package com.flipkart.krystal.utils;

import java.util.concurrent.CompletableFuture;

public final class Futures {

  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    from.whenComplete(
        (unused, throwable) -> {
          if (from.isDone() && !to.isDone()) {
            to.cancel(true);
          }
        });
  }

  public static <T> void propagateCompletion(
      CompletableFuture<? extends T> from, CompletableFuture<T> to) {
    from.whenComplete(
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(error);
          } else {
            to.complete(result);
          }
        });
  }

  /**
   * Sets up completable Futures such that
   *
   * <ul>
   *   when {@code sourceFuture} completes, its completion is propagated to {@code
   *   destinationFuture}.
   * </ul>
   *
   * <ul>
   *   when {@code destinationFuture} is cancelled, {@code sourceFuture is cancelled}
   * </ul>
   *
   * Calling this method is equivalent to calling {@link #propagateCompletion(CompletableFuture,
   * CompletableFuture) propagateCompletion(sourceFuture, destinationFuture)} and {@link
   * #propagateCancellation(CompletableFuture, CompletableFuture)
   * propagateCancellation(destinationFuture, sourceFuture)}
   */
  public static <T> void linkFutures(
      CompletableFuture<? extends T> sourceFuture, CompletableFuture<T> destinationFuture) {
    propagateCompletion(sourceFuture, destinationFuture);
    propagateCancellation(destinationFuture, sourceFuture);
  }

  private Futures() {}
}
