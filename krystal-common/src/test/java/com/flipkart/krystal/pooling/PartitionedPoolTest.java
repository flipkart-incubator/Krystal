package com.flipkart.krystal.pooling;

import static org.assertj.core.api.Assertions.assertThat;

import com.flipkart.krystal.pooling.PartitionedPool.PooledObject;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionedPoolTest {

  @Test
  void availableCount() {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(10);

    assertThat(partitionedPool.availableCount()).isEqualTo(0);
    for (int i = 0; i < 10; i++) {
      partitionedPool.leaseAndAdd(new Object());
      assertThat(partitionedPool.availableCount()).isEqualTo(i + 1);
    }
  }

  @Test
  void totalCount() throws LeaseUnavailableException {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(2);

    assertThat(partitionedPool.totalCount()).isEqualTo(0);
    for (int i = 0; i < 10; i++) {
      partitionedPool.leaseAndAdd(new Object());
      assertThat(partitionedPool.totalCount()).isEqualTo(i + 1);
    }

    for (int i = 0; i < 10; i++) {
      partitionedPool.getForLeasing(0);
      assertThat(partitionedPool.totalCount()).isEqualTo(10);
    }
  }

  @Test
  void getForLeasing_returnsObjectAtIndex() throws LeaseUnavailableException {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(10);

    for (int i = 0; i < 10; i++) {
      Object pooledObject = new Object();
      partitionedPool.leaseAndAdd(pooledObject);

      PooledObject<Object> leasedObject = partitionedPool.getForLeasing(i);
      assertThat(leasedObject.ref()).isEqualTo(pooledObject);
    }
  }

  @Test
  void getForLeasing_throwsIfObjectNotAvailable() {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(1);
    partitionedPool.leaseAndAdd(new Object());

    assertThat(partitionedPool.availableCount()).isEqualTo(0);
    try {
      partitionedPool.getForLeasing(0);
    } catch (Exception e) {
      assertThat(e)
          .isInstanceOf(LeaseUnavailableException.class)
          .hasMessage("Index [0] is not available for leasing");
    }
  }

  @Test
  void closeLease_makesObjectAvailable() throws LeaseUnavailableException {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(1);
    PooledObject<Object> pooledObject = partitionedPool.leaseAndAdd(new Object());

    assertThat(partitionedPool.availableCount()).isEqualTo(0);

    partitionedPool.closeLease(pooledObject);

    assertThat(partitionedPool.availableCount()).isEqualTo(1);
    assertThat(partitionedPool.getForLeasing(0)).isEqualTo(pooledObject);
  }

  @Test
  void closeLease_multipleObjects_makesObjectAvailable() {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(1);
    @SuppressWarnings("unchecked")
    PooledObject<Object>[] pooledObjects = new PooledObject[10];
    for (int i = 0; i < 10; i++) {
      pooledObjects[i] = partitionedPool.leaseAndAdd(new Object());
    }
    assertThat(partitionedPool.availableCount()).isEqualTo(0);
    assertThat(partitionedPool.totalCount()).isEqualTo(10);

    for (int i = 0; i < 10; i++) {
      partitionedPool.closeLease(pooledObjects[i]);
      assertThat(partitionedPool.availableCount()).isEqualTo(i + 1);
    }
  }

  @Test
  void leaseAndCloseLoop() throws LeaseUnavailableException {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(1);

    assertThat(partitionedPool.availableCount()).isEqualTo(0);
    for (int i = 0; i < 10; i++) {
      partitionedPool.closeLease(partitionedPool.leaseAndAdd(new Object()));
    }
    assertThat(partitionedPool.availableCount()).isEqualTo(10);

    for (int i = 0; i < 10; i++) {
      PooledObject<Object> leasedObject = partitionedPool.getForLeasing(0);
      assertThat(partitionedPool.availableCount()).isEqualTo(9);
      partitionedPool.closeLease(leasedObject);
      assertThat(partitionedPool.availableCount()).isEqualTo(10);
    }
  }

  @Test
  void leaseLoopAndCloseLoop() throws LeaseUnavailableException {
    PartitionedPool<Object> partitionedPool = new PartitionedPool<>(1);

    List<PooledObject<Object>> leasedObjects = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      leasedObjects.add(partitionedPool.leaseAndAdd(new Object()));
      assertThat(partitionedPool.availableCount()).isEqualTo(0);
    }
    for (int i = 0; i < 10; i++) {
      assertThat(partitionedPool.availableCount()).isEqualTo(i);
      partitionedPool.closeLease(leasedObjects.get(i));
      assertThat(partitionedPool.availableCount()).isEqualTo(i + 1);
    }
  }
}
