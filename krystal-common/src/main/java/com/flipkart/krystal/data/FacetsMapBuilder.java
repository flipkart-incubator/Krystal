package com.flipkart.krystal.data;

import com.flipkart.krystal.except.IllegalModificationException;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class FacetsMapBuilder implements FacetsBuilder {

  private final SimpleRequestBuilder<Object> request;
  private final Map<Integer, FacetValue<Object>> data;

  public FacetsMapBuilder(SimpleRequestBuilder<Object> request) {
    this(request, request._asMap());
  }

  FacetsMapBuilder(
      SimpleRequestBuilder<Object> request, Map<Integer, ? extends FacetValue<Object>> data) {
    this.request = request._asBuilder();
    this.data = new LinkedHashMap<>(data);
  }

  @Override
  public FacetValue<Object> _get(int facetId) {
    if (request._hasValue(facetId)) {
      Errable<Object> v = request._get(facetId);
      if (v != null) {
        return v;
      } else {
        throw new AssertionError("This should not be possible sinve _hasValue is true");
      }
    }
    return data.getOrDefault(facetId, Errable.nil());
  }

  @Override
  public Errable<Object> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._get(facetId);
    } else {
      FacetValue<Object> datum = data.getOrDefault(facetId, Errable.nil());
      if (datum instanceof Errable<Object> errable) {
        return errable;
      } else {
        throw new IllegalArgumentException("%s is not of type Errable".formatted(facetId));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public Responses<Request<Object>, Object> _getDepResponses(int facetId) {
    FacetValue<Object> datum = data.getOrDefault(facetId, Errable.nil());
    if (datum instanceof Responses<?, ?> errable) {
      return (Responses<Request<Object>, Object>) errable;
    } else {
      throw new IllegalArgumentException("%s is not of type Responses".formatted(facetId));
    }
  }

  @Override
  public Map<Integer, FacetValue<Object>> _asMap() {
    Map<Integer, FacetValue<Object>> map = new LinkedHashMap<>(request._asMap());
    map.putAll(data);
    return map;
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsMap _build() {
    return new FacetsMap(request, ImmutableMap.copyOf(_asMap()));
  }

  @Override
  public FacetsMapBuilder _newCopy() {
    return new FacetsMapBuilder(request, new LinkedHashMap<>(data));
  }

  @Override
  public FacetsMapBuilder _set(int facetId, FacetValue<?> value) {
    if (this._hasValue(facetId)) {
      throw new IllegalModificationException();
    }
    data.put(facetId, (FacetValue<Object>) value);
    return this;
  }

  @Override
  public FacetsMapBuilder _asBuilder() {
    return this;
  }
}
