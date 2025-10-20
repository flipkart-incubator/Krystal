package com.flipkart.krystal.pooling;

import com.flipkart.krystal.pooling.MultiLeasePoolStatsImpl.MultiLeasePoolStatsImplBuilder;
import com.flipkart.krystal.pooling.PartitionedPool.PooledObject;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A pool which leases objects randomly instead of fairly.
 *
 * @param <T> The type of the object being pooled
 */
public class RandomMultiLeasePool<T> implements MultiLeasePool<T> {

  private final PartitionedPool<T> pool;
  private final Consumer<T> destroyer;
  private final Random random = new Random();
  private final MultiLeasePoolStatsImplBuilder stats = MultiLeasePoolStatsImpl.builder();

  private final String poolName;
  private final Supplier<@NonNull T> creator;
  private final int softMaxObjects;
  private boolean closed;

  /**
   * @param creator This is used to create new objects when the pool does not have any available
   *     objects and softMaxObjects has not been breached
   * @param hardMaxLeasesPerObject The maximum number of leases that can be held for a single
   *     object. This is guaranteed to never be breached
   * @param softMaxObjects The maximum number of objects that can be created by the pool. This limit
   *     is adhered to on a best-effort basis. There might be rare scenarios where this limit is
   *     breached a little bit.
   */
  public RandomMultiLeasePool(
      String poolName,
      Supplier<@NonNull T> creator,
      int hardMaxLeasesPerObject,
      int softMaxObjects,
      Consumer<T> destroyer) {
    this.poolName = poolName;
    this.creator = creator;
    this.softMaxObjects = softMaxObjects;
    this.pool = new PartitionedPool<>(hardMaxLeasesPerObject);
    this.destroyer = destroyer;
  }

  /**
   * @return a lease to an object which is randomly chosen from the available objects and is
   *     guaranteed to have less than hardMaxLeasesPerObject leases active
   * @throws LeaseUnavailableException if softMaxObjects has been breached and all objects have
   *     hardMaxLeasesPerObject leases active
   */
  @Override
  public Lease<T> lease() throws LeaseUnavailableException {
    if (closed) {
      throw new IllegalStateException("Pool has already been closed.");
    }
    PartitionedPool.PooledObject<T> leasable;
    synchronized (pool) {
      int availableCount = pool.availableCount();
      if (availableCount == 0) {
        leasable = createNewLeasable();
      } else {
        leasable = pool.getForLeasing(random.nextInt(availableCount));
      }
      stats.reportNewLease(leasable.activeLeases());
      return new LeaseImpl<>(
          leasable,
          toClose -> {
            synchronized (pool) {
              pool.closeLease(toClose);
              if (closed && toClose.activeLeases() == 0) {
                destroyer.accept(toClose.ref());
              }
            }
            stats.reportLeaseClosed();
          });
    }
  }

  /**
   * @return a new leasable if softMaxObjects has not been breached
   * @throws LeaseUnavailableException if softMaxObjects has been breached
   */
  private PartitionedPool.PooledObject<T> createNewLeasable() throws LeaseUnavailableException {
    if (pool.totalCount() >= softMaxObjects) {
      throw new LeaseUnavailableException(
          "No more leases available in "
              + poolName
              + ". Reached max object limit: "
              + softMaxObjects);
    }
    PartitionedPool.PooledObject<T> toLease = pool.leaseAndAdd(creator.get());

    stats.reportNewObject();
    return toLease;
  }

  @Override
  public void close() {
    this.closed = true;
    if (pool.availableCount() > 0) {
      synchronized (pool) {
        if (pool.availableCount() > 0) {
          for (PooledObject<T> pooledObject : pool) {
            if (pooledObject.activeLeases() == 0) {
              // Don't destroy objects which still have active leases
              destroyer.accept(pooledObject.ref());
            } else {
              // Break since we know that all objects after this will have active leases and are
              // unavailable (PartitionedPool is implemented this way)
              break;
            }
          }
        }
      }
    }
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public MultiLeasePoolStats stats() {
    return stats.build();
  }

  private static class LeaseImpl<T> implements Lease<T> {
    private PartitionedPool.@Nullable PooledObject<T> pooledObject;
    private final Consumer<PartitionedPool.PooledObject<T>> closeLogic;

    private LeaseImpl(
        PartitionedPool.@NonNull PooledObject<T> pooledObject,
        Consumer<PartitionedPool.PooledObject<T>> closeLogic) {
      this.pooledObject = pooledObject;
      this.closeLogic = closeLogic;
    }

    @Override
    public @NonNull T get() {
      if (pooledObject == null) {
        throw new IllegalStateException("Lease already released");
      }
      return pooledObject.ref();
    }

    @Override
    public void close() {
      PartitionedPool.PooledObject<T> pooledObject = this.pooledObject;
      if (pooledObject != null) {
        closeLogic.accept(pooledObject);
        this.pooledObject = null;
      }
    }
  }
}
