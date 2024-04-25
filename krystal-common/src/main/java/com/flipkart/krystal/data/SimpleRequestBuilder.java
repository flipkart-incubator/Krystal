package com.flipkart.krystal.data;

import static com.flipkart.krystal.data.Errable.empty;

import com.flipkart.krystal.except.IllegalModificationException;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class SimpleRequestBuilder<T> extends RequestBuilder<T> {
  private final Map<Integer, Errable<Object>> data;

  public SimpleRequestBuilder() {
    this(new LinkedHashMap<>());
  }

  public SimpleRequestBuilder(Map<Integer, Errable<Object>> data) {
    this.data = data;
  }

  @Override
  public <V> Errable<V> _get(int facetId) {
    return (Errable<V>) data.getOrDefault(facetId, empty());
  }

  @Override
  public <V> Errable<V> _getErrable(int facetId) {
    return (Errable<V>) data.getOrDefault(facetId, empty());
  }

  @Override
  public <R extends Request<V>, V> Responses<R, V> _getDepResponses(int facetId) {
    return Results.empty();
  }

  @Override
  public Map<Integer, Errable<Object>> _asMap() {
    return data;
  }

  @Override
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
  public RequestBuilder<T> _newCopy() {
    return new SimpleRequestBuilder<>(new LinkedHashMap<>(data));
  }
}
