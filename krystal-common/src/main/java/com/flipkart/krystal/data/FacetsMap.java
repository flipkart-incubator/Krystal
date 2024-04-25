package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

@SuppressWarnings("unchecked")
public final class FacetsMap extends ImmutableFacets {

  private final ImmutableRequest<Object> request;
  private final ImmutableMap<Integer, FacetValue<Object>> data;

  public FacetsMap(Request<Object> request) {
    this(request, ImmutableMap.of());
  }

  public FacetsMap(Request<Object> request, ImmutableMap<Integer, FacetValue<Object>> otherFacets) {
    this.request = request._build();
    this.data = otherFacets;
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
  public <R extends Request<V>, V> Responses<R, V> _getDepResponses(int facetId) {
    FacetValue<Object> datum = data.getOrDefault(facetId, Errable.empty());
    if (datum instanceof Responses<?, ?> errable) {
      return (Responses<R, V>) errable;
    } else {
      throw new IllegalArgumentException("%s is not of type Responses".formatted(facetId));
    }
  }

  @Override
  public ImmutableMap<Integer, FacetValue<Object>> _asMap() {
    ImmutableMap<Integer, Errable<Object>> requestMap = request._asMap();
    return ImmutableMap.<Integer, FacetValue<Object>>builder()
        .putAll(requestMap)
        .putAll(data)
        .build();
  }

  @Override
  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsBuilder _asBuilder() {
    return new FacetsMapBuilder(request, data);
  }

  @Override
  public ImmutableRequest<Object> _asRequest() {
    return request;
  }
}
