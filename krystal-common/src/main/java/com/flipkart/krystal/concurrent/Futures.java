package com.flipkart.krystal.concurrent;

import static com.flipkart.krystal.except.StackTracelessCancellationException.stackTracelessCancellation;
import static com.flipkart.krystal.except.StackTracelessException.stackTracelessWrap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public final class Futures {
  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    propagateCancellation(from, to, null);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public static void propagateCancellation(
      CompletableFuture<?> from,
      CompletableFuture<?> to,
      @Nullable ExecutorService executorService) {
    BiConsumer<@Nullable Object, Throwable> action =
        (unused, throwable) -> {
          if (from.isDone() && !to.isDone()) {
            to.completeExceptionally(stackTracelessCancellation());
          }
        };
    if (executorService == null) {
      from.whenComplete(action);
    } else {
      from.whenCompleteAsync(action, executorService);
    }
  }

  public static <T> void propagateCompletion(
      CompletableFuture<? extends @PolyNull T> from, CompletableFuture<@PolyNull T> to) {
    propagateCompletion(from, to, null);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void propagateCompletion(
      CompletableFuture<? extends @PolyNull T> from,
      CompletableFuture<@PolyNull T> to,
      @Nullable ExecutorService executorService) {
    BiConsumer<@PolyNull T, Throwable> action =
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(stackTracelessWrap(error));
          } else {
            to.complete(result);
          }
        };
    if (executorService == null) {
      from.whenComplete(action);
    } else {
      from.whenCompleteAsync(action, executorService);
    }
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
    linkFutures(sourceFuture, destinationFuture, null);
  }

  /**
   * Makes sure the completion and cancellation propagation happen in the provided executor service
   */
  public static <T> void linkFutures(
      CompletableFuture<? extends @PolyNull T> sourceFuture,
      CompletableFuture<@PolyNull T> destinationFuture,
      @Nullable ExecutorService executorService) {
    propagateCompletion(sourceFuture, destinationFuture, executorService);
    propagateCancellation(destinationFuture, sourceFuture, executorService);
  }

  private Futures() {}
}
