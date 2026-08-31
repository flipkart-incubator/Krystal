package com.flipkart.krystal.concurrent;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;

import com.flipkart.krystal.except.KrystalCancellationException;
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
            to.completeExceptionally(new KrystalCancellationException());
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
            to.completeExceptionally(wrapAsCompletionException(error));
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

  /** Propagates completion of a {@link Continuation} into another {@link Continuation}. */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void propagateCompletion(
      Continuation<? extends @Nullable T> from, Continuation<@Nullable T> to) {
    from.whenComplete(
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(wrapAsCompletionException(error));
          } else {
            to.complete(result);
          }
        });
  }

  /**
   * Propagates completion of a {@link Continuation} into a {@link CompletableFuture}. Used at
   * decorator boundaries where the internal {@link Continuation} feeds an external caller.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void propagateCompletion(
      Continuation<? extends @Nullable T> from, CompletableFuture<@Nullable T> to) {
    from.whenComplete(
        (result, error) -> {
          if (error != null) {
            to.completeExceptionally(wrapAsCompletionException(error));
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

  /**
   * Propagates the completion of a {@link CompletableFuture} into a {@link Continuation}. Used when
   * cached futures need to complete internal graph-execution continuations.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void propagateCompletion(
      CompletableFuture<? extends @Nullable T> from, Continuation<@Nullable T> to) {
    from.whenComplete(
        (result, error) -> {
          if (!to.isDone()) {
            if (error != null) {
              to.completeExceptionally(wrapAsCompletionException(error));
            } else {
              to.complete(result);
            }
          }
        });
  }

  /**
   * Links a {@link Continuation} source into a {@link Continuation} destination: completion
   * propagates forward; when destination finishes, source is cancelled if still pending.
   */
  public static <T> void linkFutures(
      Continuation<? extends @Nullable T> sourceContinuation,
      Continuation<@Nullable T> destinationContinuation) {
    propagateCompletion(sourceContinuation, destinationContinuation);
    destinationContinuation.whenComplete(
        (unused, t) -> {
          if (!sourceContinuation.isDone()) {
            sourceContinuation.completeExceptionally(new KrystalCancellationException());
          }
        });
  }

  /**
   * Links a {@link Continuation} source into a {@link CompletableFuture} destination. Used at
   * decorator boundaries.
   */
  public static <T> void linkFutures(
      Continuation<? extends @Nullable T> sourceContinuation,
      CompletableFuture<@Nullable T> destinationFuture) {
    propagateCompletion(sourceContinuation, destinationFuture);
    propagateCancellation(destinationFuture, sourceContinuation.toCompletableFuture());
  }

  /**
   * Links a {@link CompletableFuture} source into a {@link Continuation} destination. Used when an
   * async I/O result (CF) feeds an internal graph-execution {@link Continuation}.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void linkFutures(
      CompletableFuture<? extends @Nullable T> sourceFuture,
      Continuation<@Nullable T> destinationContinuation) {
    sourceFuture.whenComplete(
        (result, error) -> {
          if (!destinationContinuation.isDone()) {
            if (error != null) {
              destinationContinuation.completeExceptionally(wrapAsCompletionException(error));
            } else {
              destinationContinuation.complete(result);
            }
          }
        });
  }

  /**
   * Links a {@link CompletableFuture} source into a {@link Continuation} destination, running the
   * callback on the provided executor. Used at I/O boundaries.
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  public static <T> void linkFutures(
      CompletableFuture<? extends @Nullable T> sourceFuture,
      Continuation<@Nullable T> destinationContinuation,
      @Nullable ExecutorService executorService) {
    if (executorService == null) {
      linkFutures(sourceFuture, destinationContinuation);
      return;
    }
    sourceFuture.whenCompleteAsync(
        (result, error) -> {
          if (!destinationContinuation.isDone()) {
            if (error != null) {
              destinationContinuation.completeExceptionally(wrapAsCompletionException(error));
            } else {
              destinationContinuation.complete(result);
            }
          }
        },
        executorService);
  }

  private Futures() {}
}
