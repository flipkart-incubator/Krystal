package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SimpleRequest<T> extends ImmutableRequest<T> {

  private final SimpleRequestBuilder<T> data;

  public static <T> SimpleRequest<T> empty() {
    return new SimpleRequest<>(ImmutableMap.of());
  }

  SimpleRequest(Map<Integer, Errable<Object>> data) {
    this.data = new SimpleRequestBuilder<>(ImmutableMap.copyOf(data));
  }

  @Override
  public <V> Errable<V> _get(int facetId) {
    return data._get(facetId);
  }

  @Override
  public <V> Errable<V> _getErrable(int facetId) {
    return data._getErrable(facetId);
  }

  @Override
  public <R extends Request<V>, V> Responses<R, V> _getDepResponses(int facetId) {
    return data._getDepResponses(facetId);
  }

  @Override
  public ImmutableMap<Integer, Errable<Object>> _asMap() {
    return ImmutableMap.copyOf(data._asMap());
  }

  @Override
  public boolean _hasValue(int facetId) {
    return data._hasValue(facetId);
  }

  @Override
  public RequestBuilder<T> _asBuilder() {
    return new SimpleRequestBuilder<>(new LinkedHashMap<>(data._asMap()));
  }
}
