package com.flipkart.krystal.utils;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class ImmutableMapView<K, V> implements Map<K, V> {

  private static final ImmutableMapView<?, ?> EMPTY = viewOf(ImmutableMap.of());

  private final Map<K, V> delegate;

  private ImmutableMapView(Map<K, V> delegate) {
    this.delegate = delegate;
  }

  public static <K, V> ImmutableMapView<K, V> viewOf(Map<K, V> map) {
    return new ImmutableMapView<>(map);
  }

  public static <K, V> ImmutableMapView<K, V> of() {
    //noinspection unchecked
    return (ImmutableMapView<K, V>) EMPTY;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return delegate.get(key);
  }

  @Override
  public Set<K> keySet() {
    return delegate.keySet();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public Collection<V> values() {
    return delegate.values();
  }

  @Override
  public V put(K key, V value) {
    throw uoe();
  }

  @Deprecated
  @Override
  public V remove(Object key) {
    throw uoe();
  }

  @Override
  @Deprecated
  public void putAll(Map<? extends K, ? extends V> m) {
    throw uoe();
  }

  @Override
  @Deprecated
  public void clear() {
    throw uoe();
  }

  private static UnsupportedOperationException uoe() {
    throw new UnsupportedOperationException();
  }
}
