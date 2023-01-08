package com.flipkart.krystal.utils;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class ImmutableMapView<K, V> extends AbstractMap<K, V> {

  private static final Predicate<Object> DEFAULT_KEY_FILTER = o -> true;
  private static final ImmutableMapView<?, ?> EMPTY = copyOf(ImmutableMap.of());

  private final Map<K, V> delegate;
  private final Predicate<Object> keyFilter;
  private final int size;
  private ImmutableSet<K> keySet;
  private ImmutableSet<Entry<K, V>> entrySet;
  private ImmutableList<V> values;

  private ImmutableMapView(Map<K, V> delegate, Predicate<Object> keyFilter) {
    this.delegate = delegate;
    this.keyFilter = keyFilter;
    this.size = Math.toIntExact(delegate.keySet().stream().filter(keyFilter).count());
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
    return size;
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(key) && keyFilter.test(key);
  }

  @Override
  public V get(Object key) {
    if (!keyFilter.test(key)) {
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
      this.keySet = delegate.keySet().stream().filter(keyFilter).collect(toImmutableSet());
    }
    return keySet;
  }

  @Override
  public ImmutableSet<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      this.entrySet =
          delegate.entrySet().stream()
              .filter(e -> keyFilter.test(e.getKey()))
              .collect(toImmutableSet());
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
