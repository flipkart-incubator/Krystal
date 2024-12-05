package com.flipkart.krystal.concurrent;

import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.PolyNull;

public final class Futures {

  @SuppressWarnings("FutureReturnValueIgnored")
  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    from.whenComplete(
        (unused, throwable) -> {
          if (from.isDone() && !to.isDone()) {
            to.cancel(true);
          }
        });
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void propagateCompletion(
      CompletableFuture<? extends @PolyNull T> from, CompletableFuture<@PolyNull T> to) {
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
      CompletableFuture<? extends @PolyNull T> sourceFuture,
      CompletableFuture<@PolyNull T> destinationFuture) {
    propagateCompletion(sourceFuture, destinationFuture);
    propagateCancellation(destinationFuture, sourceFuture);
  }

  private Futures() {}
}
