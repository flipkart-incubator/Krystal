package com.flipkart.krystal.krystex.testfixtures;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ErrableFacetValue;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.except.IllegalModificationException;
import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class SimpleRequestBuilder<T> implements ImmutableRequest.Builder<T> {

  private final ImmutableSet<? extends InputMirror> _facets;
  private final Map<Integer, ErrableFacetValue<Object>> _data;
  private final VajramID _vajramID;

  public SimpleRequestBuilder(Set<? extends InputMirror> _facets, VajramID vajramID) {
    this(_facets, new LinkedHashMap<>(), vajramID);
  }

  public SimpleRequestBuilder(
      Set<? extends InputMirror> _facets, Map<Integer, Errable<Object>> data, VajramID vajramID) {
    this._facets = ImmutableSet.copyOf(_facets);
    this._data =
        data.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, e -> new ErrableFacetValue<>(e.getValue())));
    this._vajramID = vajramID;
  }

  public ErrableFacetValue<Object> _get(int facetId) {
    return _data.getOrDefault(facetId, ErrableFacetValue.nil());
  }

  public Map<Integer, ErrableFacetValue<Object>> _asMap() {
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
    return new SimpleImmutRequest<>(_data, _vajramID);
  }

  @SuppressWarnings("unchecked")
  public SimpleRequestBuilder<T> _set(int facetId, FacetValue value) {
    if (!(value instanceof ErrableFacetValue<?> errableFacetValue)) {
      throw new IllegalArgumentException(
          "Expected Errable but found %s".formatted(value.getClass()));
    }
    if (_data.containsKey(facetId)) {
      throw new IllegalModificationException();
    }
    _data.put(facetId, (ErrableFacetValue<Object>) errableFacetValue);
    return this;
  }

  @Override
  public SimpleRequestBuilder<T> _newCopy() {
    return new SimpleRequestBuilder<>(
        _facets,
        new LinkedHashMap<>(
            _data.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().asErrable()))),
        _vajramID);
  }

  @Override
  public SimpleRequestBuilder<T> _asBuilder() {
    return this;
  }

  public VajramID _vajramID() {
    return _vajramID;
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
