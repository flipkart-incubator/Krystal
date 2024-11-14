package com.flipkart.krystal.pooling;

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A pool of objects that are partitioned into two sets: available and unavailable. The pool is
 * sorted in a way that all available objects are at the beginning of the list and all unavailable
 * objects are at the end of the list.
 *
 * @implNote This class is not thread safe. CLients are expected to synchronize externally
 * @param <T>
 */
class PartitionedPool<T> {

  private final int hardMaxLeasesPerObject;

  private final List<PooledObject<T>> elements = new ArrayList<>();

  private int unavailableStartIndex = 0;

  PartitionedPool(int hardMaxLeasesPerObject) {
    this.hardMaxLeasesPerObject = hardMaxLeasesPerObject;
  }

  int availableCount() {
    return unavailableStartIndex;
  }

  PooledObject<T> getForLeasing(int i) {
    if (i >= unavailableStartIndex) {
      throw new IllegalArgumentException("Index [" + i + "] is not available for leasing");
    }
    PooledObject<T> toLease = elements.get(i);
    toLease.incrementActiveLeases();
    if (toLease.activeLeases() >= hardMaxLeasesPerObject) {
      makeUnavailable(toLease.index());
    }
    return toLease;
  }

  void closeLease(PooledObject<T> toClose) {
    toClose.decrementActiveLeases();
    if (toClose.activeLeases() < hardMaxLeasesPerObject && !isAvailable(toClose)) {
      makeAvailable(toClose.index());
    }
  }

  private boolean isAvailable(PooledObject<T> element) {
    return element.index() < unavailableStartIndex;
  }

  private void makeAvailable(int indexToMakeAvailable) {
    final int unavailableStartIndex = this.unavailableStartIndex;

    if (indexToMakeAvailable < unavailableStartIndex || unavailableStartIndex >= elements.size()) {
      return;
    }
    PooledObject<T> toMove = elements.get(indexToMakeAvailable);
    PooledObject<T> other = elements.get(unavailableStartIndex);
    elements.set(indexToMakeAvailable, other);
    elements.set(unavailableStartIndex, toMove);
    toMove.index(unavailableStartIndex);
    other.index(indexToMakeAvailable);
    this.unavailableStartIndex++;
  }

  private void makeUnavailable(int indexToMakeUnavailable) {
    checkArgument(indexToMakeUnavailable >= 0, "Index to make unavailable should be >= 0");
    checkArgument(
        indexToMakeUnavailable < elements.size(),
        "Index to make unavailable should be < elements.size()");

    final int unavailableStartIndex = this.unavailableStartIndex;

    if (indexToMakeUnavailable >= unavailableStartIndex) {
      // This index is already unavailable!
      return;
    }
    PooledObject<T> toMove = elements.get(indexToMakeUnavailable);
    int otherIndex = unavailableStartIndex - 1;
    PooledObject<T> other = elements.get(otherIndex);
    elements.set(indexToMakeUnavailable, other);
    elements.set(otherIndex, toMove);
    toMove.index(unavailableStartIndex);
    other.index(indexToMakeUnavailable);
    this.unavailableStartIndex--;
  }

  public int totalSize() {
    return elements.size();
  }

  void leaseAndAdd(PooledObject<T> toLease) {
    toLease.index(elements.size());
    toLease.incrementActiveLeases();
    elements.add(toLease);
    if (isAvailable(toLease)) {
      makeAvailable(toLease.index());
    }
  }

  @Accessors(fluent = true)
  @Getter(PRIVATE)
  @RequiredArgsConstructor
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
