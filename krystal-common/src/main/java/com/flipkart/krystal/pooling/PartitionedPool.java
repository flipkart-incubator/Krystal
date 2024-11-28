package com.flipkart.krystal.pooling;

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import com.flipkart.krystal.pooling.PartitionedPool.PooledObject;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.Iterator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A pool of objects that are partitioned into two groups: available and unavailable. When an
 * available object has been leased out {@link #hardMaxLeasesPerObject} at a given point in time, it
 * is made unavailable. When an unavailable object's lease is closed, it is made available again.
 *
 * <p>This class is not thread safe. Clients are expected to synchronize externally.
 *
 * @implNote This class maintains both available and unavailable objects in a single array list
 *     which is kept sorted such that all available objects are at the beginning of the list and all
 *     unavailable objects are at the end of the list. This design is chosen for two reasons:
 *     <p>1. The operation of retrieving an object for leasing can be performed in O(1) by choosing
 *     a random integer
 *     <p>2. The operation of making an available object unavailable or vice versa can be achieved
 *     in O(1) time by swapping the objects with an appropriate index.
 *     <p>For example, let's say the pool contents are as follows:
 *     <p>[o1(a), o2(a), o3(a), o4(u), o5(u), o6(u)] where o1, o2, o3 are available and o4, o5, o6
 *     are unavailable. If we want to make o2 unavailable, we can swap o2 with o3 and update the
 *     index of o2 and o3. The pool will now look like this: [o1(a), o3(a), o2(u), o4(u), o5(u),
 *     o6(u)].
 *     <p>Similarly, in the original list, if we want to make o6 available, we can swap o6 with o4
 *     and update the index of o4 and o6. The pool will now look like this:
 *     <p>[o1(a), o2(a), o3(a), o6(a), o5(u), o4(u)].
 * @param <T>
 */
class PartitionedPool<T> implements Iterable<PooledObject<T>> {

  private final int hardMaxLeasesPerObject;

  private final ArrayList<PooledObject<T>> partitionedList = new ArrayList<>();

  /**
   * The index of the first unavailable object in the list. All objects before this index are
   * guaranteed to be available and all objects after and including this index are guaranteed to be
   * unavailable.
   *
   * <p>Special Cases:
   *
   * <ul>
   *   <li>If the list is empty, or all the elements are unavailable, this index is 0.
   *   <li>If all the elements of the list are available, the index is equal to the size of the
   *       list.
   * </ul>
   */
  private int unavailableStartIndex = 0;

  /**
   * @param hardMaxLeasesPerObject The maximum number of leases that can be held by a single object
   */
  PartitionedPool(int hardMaxLeasesPerObject) {
    this.hardMaxLeasesPerObject = hardMaxLeasesPerObject;
  }

  /**
   * @return The number of available objects in the pool
   */
  int availableCount() {
    return unavailableStartIndex;
  }

  /**
   * @return The total number of objects in the pool including both available and unavailable
   *     objects
   */
  public int totalCount() {
    return partitionedList.size();
  }

  /**
   * Example usage:
   *
   * <pre>{@code
   * pool.getForLeasing(random.nextInt(pool.availableCount()));
   * }</pre>
   *
   * @param i The index of the object to lease. It must lie between 0 (inclusive) and {@link
   *     #availableCount()} (exclusive)
   * @return The object at the given index
   * @throws IllegalArgumentException if the index is not available for leasing
   */
  PooledObject<T> getForLeasing(int i) throws LeaseUnavailableException {
    if (i >= unavailableStartIndex || i < 0) {
      throw new LeaseUnavailableException("Index [" + i + "] is not available for leasing");
    }
    PooledObject<T> toLease = partitionedList.get(i);
    toLease.incrementActiveLeases();
    if (toLease.activeLeases() >= hardMaxLeasesPerObject) {
      tryMakeUnavailable(toLease.index());
    }
    return toLease;
  }

  /**
   * This method must be called when a lease is closed. It decrements the active lease count of the
   * object and makes it available if it is not already available.
   *
   * @param toClose the object whose lease is being closed
   */
  void closeLease(PooledObject<T> toClose) {
    toClose.decrementActiveLeases();
    tryMakeAvailable(toClose.index());
  }

  /**
   * This method is called when a new object is created and leased out. It increments the active
   * leases of the object and adds it to the pool.
   *
   * @param toLease the object which was just created to be leased
   * @return a new pooled object
   */
  PooledObject<T> leaseAndAdd(@NonNull T toLease) {
    PooledObject<T> pooledObject = new PooledObject<>(toLease);

    // Add to the end of the list by default (in unavailable state)
    pooledObject.index(partitionedList.size());
    partitionedList.add(pooledObject);

    pooledObject.incrementActiveLeases();

    // Make the object available if possible since adding to the end of the list makes it
    // unavailable by default
    tryMakeAvailable(pooledObject.index());

    return pooledObject;
  }

  /**
   * Makes the object at the given index available if it has not reached the hard max leases.
   *
   * @implNote This method is O(1) because it only swaps the object with the object at {@link
   *     #unavailableStartIndex}. This swap is both performant and preserves the invariant that all
   *     available objects are at the beginning of the list and all unavailable objects are at the
   *     end of the list.
   * @param indexToMakeAvailable The index of the object to make available. It must lie between
   *     {@link #unavailableStartIndex} (inclusive) and {@link #partitionedList}.size() (exclusive)
   */
  private void tryMakeAvailable(int indexToMakeAvailable) {
    final int unavailableStartIndex = this.unavailableStartIndex;
    checkArgument(
        indexToMakeAvailable < partitionedList.size(),
        "Index to make unavailable should be < elements.size()");
    if (indexToMakeAvailable < unavailableStartIndex) {
      // Object at index is already available
      return;
    }
    if (partitionedList.get(indexToMakeAvailable).activeLeases >= hardMaxLeasesPerObject) {
      // Object at index is cannot be made available because it still has hard max leases active
      return;
    }

    PooledObject<T> toMove = partitionedList.get(indexToMakeAvailable);
    PooledObject<T> other = partitionedList.get(unavailableStartIndex);
    partitionedList.set(indexToMakeAvailable, other);
    partitionedList.set(unavailableStartIndex, toMove);
    toMove.index(unavailableStartIndex);
    other.index(indexToMakeAvailable);
    this.unavailableStartIndex++;
  }

  /**
   * @param indexToMakeUnavailable The index of the object to make unavailable. It must lie between
   *     0 (inclusive) and {@link #unavailableStartIndex} (exclusive)
   */
  private void tryMakeUnavailable(int indexToMakeUnavailable) {
    checkArgument(indexToMakeUnavailable >= 0, "Index to make unavailable should be >= 0");
    checkArgument(
        indexToMakeUnavailable < partitionedList.size(),
        "Index to make unavailable should be < elements.size()");

    final int unavailableStartIndex = this.unavailableStartIndex;

    if (indexToMakeUnavailable >= unavailableStartIndex) {
      // This index is already unavailable!
      return;
    }
    if (partitionedList.get(indexToMakeUnavailable).activeLeases < hardMaxLeasesPerObject) {
      // Object at index has not reached the hard max leases yet. So it cannot be made unavailable
      return;
    }
    PooledObject<T> toMove = partitionedList.get(indexToMakeUnavailable);
    int otherIndex = unavailableStartIndex - 1;
    PooledObject<T> other = partitionedList.get(otherIndex);
    partitionedList.set(indexToMakeUnavailable, other);
    partitionedList.set(otherIndex, toMove);
    toMove.index(unavailableStartIndex);
    other.index(indexToMakeUnavailable);
    this.unavailableStartIndex--;
  }

  @Override
  public UnmodifiableIterator<PooledObject<T>> iterator() {
    return ImmutableList.copyOf(partitionedList).iterator();
  }

  @Accessors(fluent = true)
  @Getter(PRIVATE)
  @RequiredArgsConstructor(access = PRIVATE)
  static final class PooledObject<T> {

    @Getter(PACKAGE)
    private final @NonNull T ref;

    @Setter(PRIVATE)
    private int index;

    @Getter(PACKAGE)
    private int activeLeases = 0;

    void incrementActiveLeases() {
      activeLeases++;
    }

    void decrementActiveLeases() {
      activeLeases--;
    }
  }
}
