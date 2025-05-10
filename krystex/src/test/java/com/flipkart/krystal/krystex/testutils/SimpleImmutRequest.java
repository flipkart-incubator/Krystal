package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableRequest;
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
  private final VajramID _vajramID;

  public static <T> SimpleImmutRequest<T> empty(VajramID vajramID) {
    return new SimpleImmutRequest<>(ImmutableMap.of(), vajramID);
  }

  SimpleImmutRequest(Map<Integer, Errable<Object>> data, VajramID vajramID) {
    this._data = ImmutableMap.copyOf(data);
    this._vajramID = vajramID;
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
    return new SimpleRequestBuilder<>(_facets(), new LinkedHashMap<>(_data), _vajramID);
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (o == this) return true;
    if (!(o instanceof SimpleImmutRequest<?> other)) return false;
    return Objects.equals(_asMap(), other._asMap())
        && Objects.equals(_vajramID(), other._vajramID());
  }

  @Override
  public int hashCode() {
    return Objects.hash(_data, _vajramID);
  }
}
