package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class ImmutableFacetsMap implements FacetsMap, ImmutableFacets {

  private final SimpleRequest<Object> request;
  private final ImmutableMap<Integer, FacetValue> data;

  public ImmutableFacetsMap(SimpleRequestBuilder<Object> request) {
    this(request, ImmutableMap.of());
  }

  public ImmutableFacetsMap(
      SimpleRequestBuilder<Object> request, ImmutableMap<Integer, FacetValue> otherFacets) {
    this.request = request._build();
    this.data = otherFacets;
  }

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
  public FanoutDepResponses _getDepResponses(int facetId) {
    FacetValue datum = data.getOrDefault(facetId, Errable.nil());
    if (datum instanceof FanoutDepResponses errable) {
      return (FanoutDepResponses) errable;
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

  public ImmutableSet<Facet> _facets() {
    return ImmutableSet.of();
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public FacetsMapBuilder _asBuilder() {
    return new FacetsMapBuilder(request._asBuilder(), data);
  }

  @Override
  public ImmutableFacetsMap _build() {
    return this;
  }

  @Override
  public ImmutableFacetsMap _newCopy() {
    return this;
  }
}
