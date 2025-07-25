package com.flipkart.krystal.utils;

import static com.flipkart.krystal.except.StackTracelessCancellationException.stackTracelessCancellation;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;

import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.PolyNull;

public final class Futures {

  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    from.whenComplete(
        (unused, throwable) -> {
          if (from.isDone() && !to.isDone()) {
            to.completeExceptionally(stackTracelessCancellation());
          }
        });
  }

  public static <T> void propagateCompletion(
      CompletableFuture<? extends @PolyNull T> from, CompletableFuture<@PolyNull T> to) {
    from.whenComplete(
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(stackTracelessWrap(error));
          } else {
            to.complete(result);
          }
        });
  }

  /**
   * Sets up completable Futures such that
   *
   * <ul>
   *   <li>when {@code sourceFuture} completes, its completion is propagated to {@code
   *       destinationFuture}.
   *   <li>when {@code destinationFuture} is completed, {@code sourceFuture} is cancelled if it is
   *       not yet completed
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
