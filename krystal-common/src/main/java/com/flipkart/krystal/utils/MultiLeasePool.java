package com.flipkart.krystal.utils;

import static java.lang.Math.max;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MultiLeasePool<T> implements AutoCloseable {

  private final Supplier<T> creator;
  private final MultiLeasePolicy leasePolicy;

  private final Consumer<T> destroyer;
  private volatile double peakAvgActiveLeasesPerObject;
  private volatile int maxPoolSize;

  private final Deque<PooledObject<T>> queue = new LinkedList<>();
  private volatile boolean closed;
  private volatile int maxActiveLeasesPerObject;

  public MultiLeasePool(Supplier<T> creator, MultiLeasePolicy leasePolicy, Consumer<T> destroyer) {
    this.creator = creator;
    this.leasePolicy = leasePolicy;
    this.destroyer = destroyer;
  }

  public final Lease<T> lease() {
    synchronized (this) {
      if (closed) {
        throw new IllegalStateException("MultiLeasePool already closed");
      }
      int count = queue.size();
      PooledObject<T> head;
      do {
        head = queue.peek();
        boolean leasable = checkLeasabilityAndRotateIfNeeded(head);
        if (leasable) {
          break;
        }
      } while (head != null && --count > 0);
      PooledObject<T> leasable;
      if (head == null || !shouldLeaseOut(head)) {
        leasable = createNewForLeasing();
      } else {
        leasable = head;
        leasable.incrementActiveLeases();
      }
      maxActiveLeasesPerObject = max(maxActiveLeasesPerObject, leasable.activeLeases());
      peakAvgActiveLeasesPerObject =
          max(
              peakAvgActiveLeasesPerObject,
              queue.stream().mapToInt(PooledObject::activeLeases).average().orElse(0));
      return new Lease<>(leasable, this::giveBack);
    }
  }

  private boolean checkLeasabilityAndRotateIfNeeded(@Nullable PooledObject<T> head) {
    if (head == null) {
      return false;
    }
    boolean shouldLeaseOut = shouldLeaseOut(head);
    boolean shouldPushToLast =
        // If this head shouldn't be leased out because it has too many leases active,
        // then we need to move it the last position in the queue so that others get a chance to be
        // leased out
        !shouldLeaseOut
            ||
            // If the object can be leased out, but we have reached max active objects, even then
            // push it to the last of the queue so that others get a chance to be leased out and the
            // leases get distributed in a round-robin fashion
            (leasePolicy instanceof DistributeLeases distributeLeases
                && distributeLeases.maxActiveObjects() == queue.size());
    if (shouldPushToLast) {
      queue.poll();
      if (!shouldDelete(head)) {
        queue.add(head);
      }
    }
    return shouldLeaseOut;
  }

  private boolean shouldDelete(@Nullable PooledObject<T> pooledObject) {
    if (pooledObject == null) {
      return false;
    }
    return pooledObject.markForDeletion
        > 100; // This number is a bit random - need to find a better way to calibrate this.
  }

  private void addLeasedToQueue(PooledObject<T> pooledObject) {
    if (leasePolicy instanceof PreferObjectReuse) {
      // Since object reuse is preferred, add the pooledObject at the head so that it is used
      // immediately for the next lease.
      queue.addFirst(pooledObject);
    } else if (leasePolicy instanceof DistributeLeases) {
      // Since lease distribution is preferred, add th pooledObject at the tail so that other
      // pooledObjects are used to subsequent leases.
      queue.addLast(pooledObject);
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private boolean shouldLeaseOut(PooledObject<T> pooledObject) {
    if (shouldDelete(pooledObject)) {
      return false;
    }
    if (leasePolicy instanceof PreferObjectReuse preferObjectReuse) {
      return pooledObject.activeLeases() < preferObjectReuse.maxActiveLeasesPerObject();
    } else if (leasePolicy instanceof DistributeLeases distributeLeases) {
      return pooledObject.activeLeases() < distributeLeases.distributionTriggerThreshold()
          || queue.size() == distributeLeases.maxActiveObjects();
    } else {
      throw new UnsupportedOperationException();
    }
  }

  private synchronized void giveBack(PooledObject<T> pooledObject) {
    leaseCloser.accept(pooledObject.ref());
    if (shouldDelete(pooledObject) && pooledObject.activeLeases() == 0) {
      destroyer.accept(pooledObject.ref());
    }
  }

  private PooledObject<T> createNewForLeasing() {
    PooledObject<T> pooledObject = new PooledObject<>(creator.get(), maxActiveLeasesPerObject());
    pooledObject.incrementActiveLeases();
    addLeasedToQueue(pooledObject);
    maxPoolSize = max(maxPoolSize, queue.size());
    return pooledObject;
  }

  public final int maxActiveLeasesPerObject() {
    return maxActiveLeasesPerObject;
  }

  public final double peakAvgActiveLeasesPerObject() {
    return peakAvgActiveLeasesPerObject;
  }

  public final int maxPoolSize() {
    return maxPoolSize;
  }

  @Override
  public void close() {
    this.closed = true;
    PooledObject<T> pooledObject;
    while ((pooledObject = queue.pollLast()) != null) {
      destroyer.accept(pooledObject.ref());
    }
  }

  public static final class Lease<T> implements AutoCloseable {

    private @Nullable PooledObject<T> pooledObject;
    private final Consumer<PooledObject<T>> giveback;

    private Lease(@NonNull PooledObject<T> pooledObject, Consumer<PooledObject<T>> giveback) {
      this.pooledObject = pooledObject;
      this.giveback = giveback;
    }

    public T get() {
      if (pooledObject == null) {
        throw new IllegalStateException("Lease already released");
      }
      return pooledObject.ref();
    }

    @Override
    public void close() {
      PooledObject<T> pooledObject = this.pooledObject;
      if (pooledObject != null) {
        giveback.accept(pooledObject);
        pooledObject.decrementActiveLeases();
        this.pooledObject = null;
      }
    }
  }

  private static final class PooledObject<T> {

    private final T ref;
    private final int deletionThreshold;
    private int activeLeases = 0;
    private int markForDeletion;

    private PooledObject(T ref, int deletionThreshold) {
      this.ref = ref;
      this.deletionThreshold = deletionThreshold;
    }

    private T ref() {
      return ref;
    }

    private int activeLeases() {
      return activeLeases;
    }

    private void incrementActiveLeases() {
      activeLeases++;
      if (activeLeases() == deletionThreshold) {
        markForDeletion = 0;
      }
    }

    private void decrementActiveLeases() {
      activeLeases--;
      if (activeLeases() < deletionThreshold) {
        markForDeletion++;
      }
    }
  }
}
