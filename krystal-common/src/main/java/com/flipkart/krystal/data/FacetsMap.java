package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public final class FacetsMap implements ImmutableFacets {

  private final SimpleRequest<Object> request;
  private final ImmutableMap<Integer, FacetValue<Object>> data;

  public FacetsMap(SimpleRequestBuilder<Object> request) {
    this(request, ImmutableMap.of());
  }

  public FacetsMap(
      SimpleRequestBuilder<Object> request, ImmutableMap<Integer, FacetValue<Object>> otherFacets) {
    this.request = request._build();
    this.data = otherFacets;
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
    return data.getOrDefault(facetId, Errable.empty());
  }

  @Override
  public Errable<Object> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._get(facetId);
    } else {
      FacetValue<Object> datum = data.getOrDefault(facetId, Errable.empty());
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
    FacetValue<Object> datum = data.getOrDefault(facetId, Errable.empty());
    if (datum instanceof Responses<?, ?> errable) {
      return (Responses<Request<Object>, Object>) errable;
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

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsBuilder _asBuilder() {
    return new FacetsMapBuilder(request._asBuilder(), data);
  }

  @Override
  public FacetsMap _build() {
    return this;
  }

  @Override
  public ImmutableFacets _newCopy() {
    return this;
  }
}
