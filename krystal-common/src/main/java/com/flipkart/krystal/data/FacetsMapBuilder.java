package com.flipkart.krystal.data;

import com.flipkart.krystal.except.IllegalModificationException;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class FacetsMapBuilder extends FacetsBuilder {

  private final RequestBuilder<Object> request;
  private final Map<Integer, FacetValue<Object>> data;

  public FacetsMapBuilder(Request<Object> request) {
    this(request, request._asMap());
  }

  FacetsMapBuilder(Request<Object> request, Map<Integer, ? extends FacetValue<Object>> data) {
    this.request = request._asBuilder();
    this.data = new LinkedHashMap<>(data);
  }

  @Override
  public <V> FacetValue<V> _get(int facetId) {
    if (request._hasValue(facetId)) {
      Errable<V> v = request._get(facetId);
      if (v != null) {
        return v;
      } else {
        throw new AssertionError("This should not be possible sinve _hasValue is true");
      }
    }
    return (FacetValue<V>) data.getOrDefault(facetId, Errable.empty());
  }

  @Override
  public <V> Errable<V> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._getErrable(facetId);
    } else {
      FacetValue<Object> datum = data.getOrDefault(facetId, Errable.empty());
      if (datum instanceof Errable<?> errable) {
        return (Errable<V>) errable;
      } else {
        throw new IllegalArgumentException("%s is not of type Errable".formatted(facetId));
      }
    }
  }

  @Override
  public <R extends Request<V>, V> Responses<R, V> _getResponses(int facetId) {
    FacetValue<Object> datum = data.getOrDefault(facetId, Errable.empty());
    if (datum instanceof Responses<?, ?> errable) {
      return (Responses<R, V>) errable;
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

  @Override
  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsMap _build() {
    return new FacetsMap(request, ImmutableMap.copyOf(_asMap()));
  }

  @Override
  public FacetsMapBuilder _newCopy() {
    return new FacetsMapBuilder(request._newCopy(), new LinkedHashMap<>(data));
  }

  @Override
  public FacetContainerBuilder _set(int facetId, FacetValue<?> value) {
    if (this._hasValue(facetId)) {
      throw new IllegalModificationException();
    }
    data.put(facetId, (FacetValue<Object>) value);
    return this;
  }

  @Override
  public RequestBuilder<Object> _asRequest() {
    return request;
  }
}
