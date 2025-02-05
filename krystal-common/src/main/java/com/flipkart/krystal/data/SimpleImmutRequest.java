package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public final class SimpleImmutRequest<T> implements SimpleRequest<T>, ImmutableRequest<T> {
  private final Map<Integer, Errable<Object>> _data;

  public static <T> SimpleImmutRequest<T> empty() {
    return new SimpleImmutRequest<>(ImmutableMap.of());
  }

  SimpleImmutRequest(Map<Integer, Errable<Object>> data) {
    this._data = ImmutableMap.copyOf(data);
  }

  public Errable<Object> _get(int facetId) {
    return _data.getOrDefault(facetId, nil());
  }

  @Override
  public SimpleImmutRequest<T> _build() {
    return this;
  }

  @Override
  public ImmutableRequest<T> _newCopy() {
    return this;
  }

  @Override
  public Map<Integer, Errable<Object>> _asMap() {
    return _data;
  }

  @Override
  public ImmutableSet<InputMirror> _facets() {
    return ImmutableSet.of();
  }

  public boolean _hasValue(int facetId) {
    return _data.containsKey(facetId);
  }

  @Override
  public SimpleRequestBuilder<T> _asBuilder() {
    return new SimpleRequestBuilder<>(_facets(), new LinkedHashMap<>(_data));
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (o == this) return true;
    if (!(o instanceof SimpleRequest<?> other)) return false;
    return Objects.equals(this._data, other._asMap());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._data);
  }
}
