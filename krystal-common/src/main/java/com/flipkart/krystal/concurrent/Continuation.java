package com.flipkart.krystal.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Lightweight single-threaded alternative to CompletableFuture. No volatile reads, no CAS, no
 * linked-list traversal. MUST only be completed and observed from the same thread
 * (SingleThreadExecutor).
 */
public final class Continuation<T> {

  private static final Object PENDING = new Object();

  @SuppressWarnings("unchecked")
  private @Nullable T result = (T) PENDING;

  private @Nullable Throwable error;
  private @Nullable List<BiConsumer<@Nullable T, @Nullable Throwable>> callbacks;

  @SuppressWarnings("unchecked")
  public boolean isDone() {
    return result != (T) PENDING || error != null;
  }

  public void complete(@Nullable T value) {
    this.result = value;
    fire();
  }

  public void completeExceptionally(Throwable e) {
    this.error = e;
    @SuppressWarnings("unchecked")
    T sentinel = (T) PENDING;
    this.result = sentinel; // keep PENDING so isDone() still works via error != null
    fire();
  }

  public boolean isCompletedExceptionally() {
    return error != null;
  }

  /** Register a callback. If already done, fires immediately (inline, no enqueue). */
  @SuppressWarnings("unchecked")
  public void whenComplete(BiConsumer<@Nullable T, @Nullable Throwable> cb) {
    if (isDone()) {
      cb.accept(result == (T) PENDING ? null : result, error);
    } else {
      if (callbacks == null) callbacks = new ArrayList<>(2); // most nodes have 1-2 dependents
      callbacks.add(cb);
    }
  }

  @SuppressWarnings("unchecked")
  private void fire() {
    if (callbacks == null) return;
    T r = result == (T) PENDING ? null : result;
    for (var cb : callbacks) cb.accept(r, error);
    callbacks = null; // release references
  }

  /**
   * Bridge to CompletableFuture for I/O boundaries (decorators, external callers). Only call this
   * when handing off to code that expects a CF.
   */
  public CompletableFuture<T> toCompletableFuture() {
    CompletableFuture<T> cf = new CompletableFuture<>();
    whenComplete(
        (v, e) -> {
          if (e != null) cf.completeExceptionally(e);
          else cf.complete(v);
        });
    return cf;
  }

  /**
   * Bridge from CompletableFuture (async I/O result) back into graph execution. The CF's thread
   * will call complete() — safe because SingleThreadExecutor.execute() checks
   * isCurrentThreadTheSingleThread() and routes accordingly.
   */
  public static <T> Continuation<T> fromCompletableFuture(
      CompletableFuture<T> cf, SingleThreadExecutor executor) {
    Continuation<T> c = new Continuation<>();
    cf.whenCompleteAsync(
        (v, e) -> {
          if (e != null) c.completeExceptionally(e);
          else c.complete(v);
        },
        executor); // executor.execute() runs inline if already on the right thread
    return c;
  }
}
