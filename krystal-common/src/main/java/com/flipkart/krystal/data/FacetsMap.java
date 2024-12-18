package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

public final class FacetsMap implements ImmutableFacets {

  private final SimpleRequest<Object> request;
  private final ImmutableMap<Integer, FacetValue> data;

  public FacetsMap(SimpleRequestBuilder<Object> request) {
    this(request, ImmutableMap.of());
  }

  public FacetsMap(
      SimpleRequestBuilder<Object> request, ImmutableMap<Integer, FacetValue> otherFacets) {
    this.request = request._build();
    this.data = otherFacets;
  }

  @Override
  public FacetValue _get(int facetId) {
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
  public Errable<?> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._get(facetId);
    } else {
      FacetValue datum = data.getOrDefault(facetId, Errable.nil());
      if (datum instanceof Errable<?> errable) {
        return errable;
      } else {
        throw new IllegalArgumentException("%s is not of type Errable".formatted(facetId));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public DependencyResponses<Request<Object>, Object> _getDepResponses(int facetId) {
    FacetValue datum = data.getOrDefault(facetId, Errable.nil());
    if (datum instanceof DependencyResponses<?, ?> errable) {
      return (DependencyResponses<Request<Object>, Object>) errable;
    } else {
      throw new IllegalArgumentException("%s is not of type Responses".formatted(facetId));
    }
  }

  @Override
  public ImmutableMap<Integer, FacetValue> _asMap() {
    return ImmutableMap.<Integer, FacetValue>builder()
        .putAll(request._asMap())
        .putAll(data)
        .build();
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsMapBuilder _asBuilder() {
    return new FacetsMapBuilder(request._asBuilder(), data);
  }

  @Override
  public FacetsMap _build() {
    return this;
  }

  @Override
  public FacetsMap _newCopy() {
    return this;
  }
}
