package com.flipkart.krystal.pooling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RandomMultiLeasePoolTest {

  @Test
  void lease_createsNewObjectWhenPoolIsEmpty() throws LeaseUnavailableException {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 3, 5, obj -> {});

    assertThat(pool.stats().currentPoolSize()).isEqualTo(0);
    Lease<Object> lease = pool.lease();
    assertThat(lease.get()).isNotNull();
    assertThat(pool.stats().currentPoolSize()).isEqualTo(1);
  }

  @Test
  void lease_createsNewObjectWhenNoObjectsAreAvailable() throws LeaseUnavailableException {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 1, 5, obj -> {});
    pool.lease();

    assertThat(pool.stats().currentPoolSize()).isEqualTo(1);
    Lease<Object> lease = pool.lease();
    assertThat(lease.get()).isNotNull();
    assertThat(pool.stats().currentPoolSize()).isEqualTo(2);
  }

  @Test
  void lease_throwsExceptionWhenSoftMaxObjectsBreached() throws LeaseUnavailableException {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 1, 5, obj -> {});

    for (int i = 0; i < 5; i++) {
      pool.lease();
    }
    assertThatThrownBy(pool::lease).isInstanceOf(LeaseUnavailableException.class);
  }

  @Test
  void lease_reusesAvailableObject() throws LeaseUnavailableException {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 3, 5, obj -> {});

    Lease<Object> lease1 = pool.lease();
    Object object1 = lease1.get();
    lease1.close();
    Lease<Object> lease2 = pool.lease();
    Object object2 = lease2.get();
    assertThat(object1).isEqualTo(object2);
  }

  @Test
  void close_makesObjectAvailableForReuse() throws LeaseUnavailableException {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 3, 5, obj -> {});

    Lease<Object> lease = pool.lease();
    lease.close();
    assertThat(pool.stats().currentPoolSize()).isEqualTo(1);
  }

  @Test
  void close_destroysObjectWhenPoolIsClosed() throws LeaseUnavailableException {
    @SuppressWarnings("LimitedScopeInnerClass")
    class Closeable {
      private boolean closed = false;

      void close() {
        closed = true;
      }
    }
    RandomMultiLeasePool<Closeable> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Closeable::new, 3, 5, Closeable::close);

    Lease<Closeable> lease = pool.lease();
    Closeable closeable = lease.get();
    pool.close();
    lease.close();
    assertThat(closeable.closed).isTrue();
  }

  @Test
  void lease_throwsExceptionWhenPoolIsClosed() {
    RandomMultiLeasePool<Object> pool =
        new RandomMultiLeasePool<>("RandomMultiLeasePool", Object::new, 3, 5, obj -> {});

    pool.close();
    assertThatThrownBy(pool::lease)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Pool has already been closed.");
  }
}
