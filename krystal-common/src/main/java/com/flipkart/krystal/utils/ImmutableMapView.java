package com.flipkart.krystal.utils;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ImmutableMapView<K, V> extends AbstractMap<K, V> {

  private static final Predicate<Object> DEFAULT_KEY_FILTER = null;
  private static final ImmutableMapView<?, ?> EMPTY = copyOf(ImmutableMap.of());

  private final Map<K, V> delegate;
  private final Predicate<Object> keyFilter;
  private Integer size;
  private Set<K> keySet;
  private Set<Entry<K, V>> entrySet;
  private ImmutableList<V> values;

  private ImmutableMapView(Map<K, V> delegate, @Nullable Predicate<Object> keyFilter) {
    this.delegate = delegate;
    this.keyFilter = keyFilter;
  }

  public static <K, V> ImmutableMapView<K, V> copyOf(Map<K, V> map) {
    return filteredCopyOf(map, DEFAULT_KEY_FILTER);
  }

  public static <K, V> ImmutableMapView<K, V> filteredCopyOf(
      Map<K, V> map, Predicate<Object> keyFilter) {
    return new ImmutableMapView<>(ImmutableMap.copyOf(map), keyFilter);
  }

  public static <K, V> ImmutableMapView<K, V> copyOf(ImmutableMapView<K, V> map) {
    return filteredCopyOf(map, DEFAULT_KEY_FILTER);
  }

  public static <K, V> ImmutableMapView<K, V> filteredCopyOf(
      ImmutableMapView<K, V> map, Predicate<Object> keyFilter) {
    return new ImmutableMapView<>(map, keyFilter);
  }

  public static <K, V> ImmutableMapView<K, V> of() {
    //noinspection unchecked
    return (ImmutableMapView<K, V>) EMPTY;
  }

  @Override
  public int size() {
    if (size == null) {
      if (keyFilter != null) {
        this.size = Math.toIntExact(delegate.keySet().stream().filter(keyFilter).count());
      } else {
        this.size = delegate.size();
      }
    }
    return size;
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(key) && (keyFilter == null || keyFilter.test(key));
  }

  @Override
  public V get(Object key) {
    if (keyFilter != null && !keyFilter.test(key)) {
      return null;
    }
    return delegate.get(key);
  }

  @Deprecated
  @Override
  public V remove(Object key) {
    return uoe();
  }

  @Override
  @Deprecated
  public void putAll(Map<? extends K, ? extends V> m) {
    uoe();
  }

  @Override
  @Deprecated
  public void clear() {
    uoe();
  }

  @Override
  public Set<K> keySet() {
    if (keySet == null) {
      if (keyFilter != null) {
        this.keySet = delegate.keySet().stream().filter(keyFilter).collect(toImmutableSet());
      } else {
        this.keySet = delegate.keySet();
      }
    }
    return keySet;
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      if (keyFilter != null) {
        this.entrySet =
            delegate.entrySet().stream()
                .filter(e -> keyFilter.test(e.getKey()))
                .collect(toImmutableSet());
      } else {
        this.entrySet = delegate.entrySet();
      }
    }
    return entrySet;
  }

  @Override
  public ImmutableCollection<V> values() {
    if (values == null) {
      //noinspection SimplifyStreamApiCallChains
      this.values = entrySet().stream().map(Entry::getValue).collect(toImmutableList());
    }
    return values;
  }

  private static <T> T uoe() {
    throw new UnsupportedOperationException();
  }
}
