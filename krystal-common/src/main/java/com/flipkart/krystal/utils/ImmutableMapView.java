package com.flipkart.krystal.utils;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;

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

  /*--------------------------Overridden delegation methods------------------------*/

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
  @NonNull
  public Set<K> keySet() {
    return delegate.keySet();
  }

  @Override
  @NonNull
  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  @NonNull
  public Collection<V> values() {
    return delegate.values();
  }

  /*--------------------------Unsupported methods throw exceptions------------------------*/

  @Deprecated
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

  @Deprecated
  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    throw uoe();
  }

  @Deprecated
  @Override
  public V putIfAbsent(K key, V value) {
    throw uoe();
  }

  @Override
  @Deprecated
  public boolean remove(Object key, Object value) {
    throw uoe();
  }

  @Override
  @Deprecated
  public boolean replace(K key, V oldValue, V newValue) {
    throw uoe();
  }

  @Override
  @Deprecated
  public V replace(K key, V value) {
    throw uoe();
  }

  @Override
  @Deprecated
  public V computeIfAbsent(K key, @NonNull Function<? super K, ? extends V> mappingFunction) {
    throw uoe();
  }

  @Override
  @Deprecated
  public V computeIfPresent(
      K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw uoe();
  }

  @Override
  @Deprecated
  public V compute(
      K key, @NonNull BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    throw uoe();
  }

  @Override
  @Deprecated
  public V merge(
      K key,
      @NonNull V value,
      @NonNull BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    throw uoe();
  }

  private static UnsupportedOperationException uoe() {
    throw new UnsupportedOperationException();
  }
}
