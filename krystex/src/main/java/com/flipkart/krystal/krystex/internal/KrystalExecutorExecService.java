package com.flipkart.krystal.krystex.internal;

import com.flipkart.krystal.krystex.KrystalExecutor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"override.return", "override.param"})
public final class KrystalExecutorExecService implements ExecutorService {

  public static ThreadLocal<@Nullable KrystalExecutor> THREAD_LOCAL = new ThreadLocal<>();

  private final ExecutorService delegate;
  private final KrystalExecutor kryonExecutor;

  public KrystalExecutorExecService(KrystalExecutor kryonExecutor, ExecutorService delegate) {
    this.kryonExecutor = kryonExecutor;
    this.delegate = delegate;
  }

  @Override
  public void shutdown() {
    delegate.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return delegate.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return delegate.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return delegate.isTerminated();
  }

  @Override
  public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
    return delegate.awaitTermination(timeout, unit);
  }

  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result) {
    return delegate.submit(wrap(task), result);
  }

  @Override
  public Future<?> submit(Runnable task) {
    return delegate.submit(wrap(task));
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
      throws InterruptedException {
    Collection<Callable<T>> mappedCollection = new ArrayList<>(tasks.size());
    tasks.forEach(tCallable -> mappedCollection.add(wrap(tCallable)));
    return delegate.invokeAll(mappedCollection);
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException {
    Collection<Callable<T>> mappedCollection = new ArrayList<>(tasks.size());
    tasks.forEach(tCallable -> mappedCollection.add(wrap(tCallable)));
    return delegate.invokeAll(mappedCollection, timeout, unit);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
      throws InterruptedException, ExecutionException {
    Collection<Callable<T>> mappedCollection = new ArrayList<>(tasks.size());
    tasks.forEach(tCallable -> mappedCollection.add(wrap(tCallable)));
    return delegate.invokeAny(mappedCollection);
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    Collection<Callable<T>> mappedCollection = new ArrayList<>(tasks.size());
    tasks.forEach(tCallable -> mappedCollection.add(wrap(tCallable)));
    return delegate.invokeAny(mappedCollection, timeout, unit);
  }

  @Override
  public void execute(Runnable command) {
    delegate.execute(wrap(command));
  }

  private <T> Callable<T> wrap(Callable<T> task) {
    return () -> {
      @Nullable KrystalExecutor oldValue = THREAD_LOCAL.get();
      THREAD_LOCAL.set(kryonExecutor);
      try {
        return task.call();
      } finally {
        THREAD_LOCAL.set(oldValue);
      }
    };
  }

  private Runnable wrap(Runnable task) {
    return () -> {
      @Nullable KrystalExecutor oldValue = THREAD_LOCAL.get();
      THREAD_LOCAL.set(kryonExecutor);
      try {
        task.run();
      } finally {
        THREAD_LOCAL.set(oldValue);
      }
    };
  }
}
