package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class SimpleRequest<T> implements ImmutableRequest<T> {

  private final SimpleRequestBuilder<T> data;

  public static <T> SimpleRequest<T> empty() {
    return new SimpleRequest<>(ImmutableMap.of());
  }

  SimpleRequest(Map<Integer, Errable<Object>> data) {
    this.data = new SimpleRequestBuilder<>(ImmutableMap.copyOf(data));
  }

  @Override
  public Errable<Object> _get(int facetId) {
    return data._get(facetId);
  }

  @Override
  public SimpleRequest<T> _build() {
    return this;
  }

  @Override
  public ImmutableRequest<T> _newCopy() {
    return this;
  }

  @Override
  public ImmutableMap<Integer, Errable<Object>> _asMap() {
    return ImmutableMap.copyOf(data._asMap());
  }

  public boolean _hasValue(int facetId) {
    return data._hasValue(facetId);
  }

  @Override
  public SimpleRequestBuilder<T> _asBuilder() {
    return new SimpleRequestBuilder<>(new LinkedHashMap<>(data._asMap()));
  }
}
