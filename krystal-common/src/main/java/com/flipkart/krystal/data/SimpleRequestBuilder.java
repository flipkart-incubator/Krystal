package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.nil;

import com.flipkart.krystal.except.IllegalModificationException;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class SimpleRequestBuilder<T> implements RequestBuilder<T> {
  private final Map<Integer, Errable<Object>> data;

  public SimpleRequestBuilder() {
    this(new LinkedHashMap<>());
  }

  public SimpleRequestBuilder(Map<Integer, Errable<Object>> data) {
    this.data = data;
  }

  @Override
  public Errable<Object> _get(int facetId) {
    return data.getOrDefault(facetId, nil());
  }

  @Override
  public Map<Integer, Errable<Object>> _asMap() {
    return data;
  }

  public boolean _hasValue(int facetId) {
    return data.containsKey(facetId);
  }

  @Override
  public SimpleRequest<T> _build() {
    return new SimpleRequest<>(data);
  }

  @Override
  public RequestBuilder<T> _set(int facetId, FacetValue<?> value) {
    if (!(value instanceof Errable<?> errable)) {
      throw new IllegalArgumentException(
          "Expected Errable but found %s".formatted(value.getClass()));
    }
    if (data.containsKey(facetId)) {
      throw new IllegalModificationException();
    }
    data.put(facetId, (Errable<Object>) errable);
    return this;
  }

  @Override
  public SimpleRequestBuilder<T> _newCopy() {
    return new SimpleRequestBuilder<>(new LinkedHashMap<>(data));
  }

  @Override
  public SimpleRequestBuilder<T> _asBuilder() {
    return this;
  }
}
