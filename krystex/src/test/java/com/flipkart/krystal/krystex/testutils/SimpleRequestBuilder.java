package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.except.IllegalModificationException;
import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@Getter
public final class SimpleRequestBuilder<T> implements Builder<T> {

  private final ImmutableSet<? extends InputMirror> _facets;
  private final Map<Integer, Errable<Object>> _data;

  public SimpleRequestBuilder(Set<? extends InputMirror> _facets) {
    this(_facets, new LinkedHashMap<>());
  }

  public SimpleRequestBuilder(
      Set<? extends InputMirror> _facets, Map<Integer, Errable<Object>> data) {
    this._facets = ImmutableSet.copyOf(_facets);
    this._data = data;
  }

  public Errable<Object> _get(int facetId) {
    return _data.getOrDefault(facetId, nil());
  }

  public Map<Integer, Errable<Object>> _asMap() {
    return _data;
  }

  @Override
  public ImmutableSet<? extends InputMirror> _facets() {
    return _facets;
  }

  public boolean _hasValue(int facetId) {
    return _data.containsKey(facetId);
  }

  @Override
  public SimpleImmutRequest<T> _build() {
    return new SimpleImmutRequest<>(_data);
  }

  @SuppressWarnings("unchecked")
  public Builder _set(int facetId, FacetValue value) {
    if (!(value instanceof Errable<?> errable)) {
      throw new IllegalArgumentException(
          "Expected Errable but found %s".formatted(value.getClass()));
    }
    if (_data.containsKey(facetId)) {
      throw new IllegalModificationException();
    }
    _data.put(facetId, (Errable<Object>) errable);
    return this;
  }

  @Override
  public SimpleRequestBuilder<T> _newCopy() {
    return new SimpleRequestBuilder<>(_facets, new LinkedHashMap<>(_data));
  }

  @Override
  public SimpleRequestBuilder<T> _asBuilder() {
    return this;
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
