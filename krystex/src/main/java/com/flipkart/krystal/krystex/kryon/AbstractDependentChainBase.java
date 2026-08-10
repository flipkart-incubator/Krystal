package com.flipkart.krystal.krystex.kryon;

import static lombok.AccessLevel.PACKAGE;
import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract sealed class AbstractDependentChainBase<T extends DependentChainBase>
    implements DependentChainBase
    permits DependentChainStart, DependentChainImpl, DependentChainSliceImpl {

  @Getter(PACKAGE)
  private final DependentChainNode[] array;

  private final @Nullable Dependency latestDependency;

  // Must use ConcurrentHashMap for thread safety - else we will encounter
  // ConcurrentModificationException
  @EqualsAndHashCode.Exclude @ToString.Exclude
  private final ConcurrentHashMap<DependentChainNode, T> dependenciesInternPool =
      new ConcurrentHashMap<>();

  private @MonotonicNonNull String toString;

  protected AbstractDependentChainBase() {
    this.latestDependency = null;
    this.array = new DependentChainNode[0];
  }

  protected AbstractDependentChainBase(
      VajramID vajramID,
      Dependency latestDependency,
      @Nullable DependentChainBase incomingDependentChain) {
    this.latestDependency = latestDependency;
    this.array = create(vajramID, latestDependency, incomingDependentChain);
  }

  @Override
  public T extend(VajramID vajramID, Dependency dependency) {
    DependentChainNode key = new DependentChainNode(vajramID, dependency);
    T dependentChain = dependenciesInternPool.get(key);
    if (dependentChain == null) {
      T newDependentChain = _extend(vajramID, dependency);
      T existing = dependenciesInternPool.putIfAbsent(key, newDependentChain);
      dependentChain = existing != null ? existing : newDependentChain;
    }
    return dependentChain;
  }

  protected abstract T _extend(VajramID vajramID, Dependency dependency);

  @Override
  public boolean endsWith(DependentChainBase dependentChain) {
    if (dependentChain instanceof AbstractDependentChainBase other) {
      DependentChainNode[] otherArray = other.array;
      if (otherArray.length > array.length) {
        return false;
      }
      for (int i = 1; i <= otherArray.length; i++) {
        // Compare in reverse because this presumably finds mismatches faster (not measured yet)
        // Functionally it doesn't matter in which order we compare
        if (!array[array.length - i].equals(otherArray[otherArray.length - i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public @Nullable Dependency latestDependency() {
    return latestDependency;
  }

  private static DependentChainNode[] create(
      VajramID vajramID,
      Dependency latestDependency,
      @Nullable DependentChainBase incomingDependentChain) {
    DependentChainNode[] incomingArray;
    if (incomingDependentChain instanceof AbstractDependentChainBase abstractDependentChain) {
      incomingArray = abstractDependentChain.array;
    } else {
      incomingArray = new DependentChainNode[0];
    }
    DependentChainNode[] array = new DependentChainNode[incomingArray.length + 1];
    System.arraycopy(incomingArray, 0, array, 0, incomingArray.length);
    array[array.length - 1] = new DependentChainNode(vajramID, latestDependency);
    return array;
  }

  @Override
  public String toString() {
    if (toString == null) {
      toString =
          (this instanceof DependentChain ? "[Start]>" : "")
              + Arrays.stream(array).map(Object::toString).collect(Collectors.joining(":"));
    }
    return toString;
  }

  /** Key class for ConcurrentHashMap */
  // We want to cache the hashcode, but @EqualsAndHashCode doesn't support
  // records (see: https://github.com/projectlombok/lombok/issues/3246)
  @SuppressWarnings("ClassCanBeRecord")
  @EqualsAndHashCode(cacheStrategy = LAZY)
  static class DependentChainNode {
    @Getter(PACKAGE)
    private final VajramID vajramId;

    @Getter(PACKAGE)
    private final Dependency dependency;

    public DependentChainNode(VajramID vajramId, Dependency dependency) {
      this.dependency = dependency;
      this.vajramId = vajramId;
    }

    @Override
    public String toString() {
      return "%s:%s".formatted(vajramId.id(), dependency.name());
    }
  }
}
