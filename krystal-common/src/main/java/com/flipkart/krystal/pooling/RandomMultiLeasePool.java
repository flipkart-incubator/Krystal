package com.flipkart.krystal.pooling;

import com.flipkart.krystal.pooling.MultiLeasePoolStatsImpl.MultiLeasePoolStatsImplBuilder;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RandomMultiLeasePool<T extends @NonNull Object> implements MultiLeasePool<T> {

  private final PartitionedPool<T> pool;
  private final Random random = new Random();
  private final MultiLeasePoolStatsImplBuilder stats = MultiLeasePoolStatsImpl.builder();

  private final Supplier<@NonNull T> creator;
  private final int softMaxObjects;

  public RandomMultiLeasePool(
      Supplier<@NonNull T> creator, int hardMaxLeasesPerObject, int softMaxObjects) {
    this.creator = creator;
    this.softMaxObjects = softMaxObjects;
    this.pool = new PartitionedPool<>(hardMaxLeasesPerObject);
  }

  @Override
  public Lease<T> lease() throws LeaseUnavailableException {
    PartitionedPool.PooledObject<T> leasable;
    synchronized (pool) {
      int availableCount = pool.availableCount();
      if (availableCount == 0) {
        leasable = creatNewLeasable();
      } else {
        leasable = pool.getForLeasing(random.nextInt(availableCount));
      }
      stats.reportNewLease(leasable.activeLeases());
      return new LeaseImpl<>(
          leasable,
          toClose -> {
            synchronized (pool) {
              stats.reportLeaseClosed();
              pool.closeLease(toClose);
            }
          });
    }
  }

  @Override
  public MultiLeasePoolStats stats() {
    return stats.build();
  }

  /**
   * @return a new leasable if softMaxObjects has not been breached
   * @throws LeaseUnavailableException if softMaxObjects has been breached
   */
  private PartitionedPool.PooledObject<T> creatNewLeasable() throws LeaseUnavailableException {
    if (pool.totalSize() >= softMaxObjects) {
      throw new LeaseUnavailableException("No more leases available");
    }
    PartitionedPool.PooledObject<T> toLease = new PartitionedPool.PooledObject<>(creator.get());
    pool.leaseAndAdd(toLease);
    stats.reportNewObject();
    return toLease;
  }

  @Override
  public void close() {}

  private static class LeaseImpl<T extends @NonNull Object> implements Lease<T> {
    private PartitionedPool.@Nullable PooledObject<T> pooledObject;
    private final Consumer<PartitionedPool.PooledObject<T>> giveback;

    private LeaseImpl(
        PartitionedPool.PooledObject<T> pooledObject,
        Consumer<PartitionedPool.PooledObject<T>> giveback) {
      this.pooledObject = pooledObject;
      this.giveback = giveback;
    }

    @Override
    public T get() {
      if (pooledObject == null) {
        throw new IllegalStateException("Lease already released");
      }
      return pooledObject.ref();
    }

    @Override
    public void close() {
      PartitionedPool.PooledObject<T> pooledObject = this.pooledObject;
      if (pooledObject != null) {
        giveback.accept(pooledObject);
        this.pooledObject = null;
      }
    }
  }
}
