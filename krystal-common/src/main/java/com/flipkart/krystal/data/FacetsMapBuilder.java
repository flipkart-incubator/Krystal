package com.flipkart.krystal.data;

import com.flipkart.krystal.except.IllegalModificationException;
import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class FacetsMapBuilder implements FacetsMap, FacetsBuilder {

  private final SimpleRequestBuilder<Object> request;
  private final Map<Integer, FacetValue> data;

  public FacetsMapBuilder(SimpleRequestBuilder<Object> request) {
    this(request, request._asMap());
  }

  FacetsMapBuilder(SimpleRequestBuilder<Object> request, Map<Integer, ? extends FacetValue> data) {
    this.request = request._asBuilder();
    this.data = new LinkedHashMap<>(data);
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
  public FanoutDepResponses _getDepResponses(int facetId) {
    FacetValue datum = data.getOrDefault(facetId, Errable.nil());
    if (datum instanceof FanoutDepResponses fanoutDepResponses) {
      return (FanoutDepResponses) fanoutDepResponses;
    } else if (datum instanceof RequestResponse requestResponse) {
      return new FanoutDepResponses(
          ImmutableList.of((RequestResponse) requestResponse));
    } else if (datum instanceof One2OneDepResponse.NoRequest noRequest) {
      return new FanoutDepResponses(
          ImmutableList.of(
              new RequestResponse(
                  SimpleRequest.empty(), (Errable<Object>) noRequest.response())));
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

  @Override
  public ImmutableSet<Facet> _facets() {
    return ImmutableSet.of();
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || data.containsKey(facetId);
  }

  @Override
  public ImmutableFacetsMap _build() {
    return new ImmutableFacetsMap(request, ImmutableMap.copyOf(_asMap()));
  }

  @Override
  public FacetsMapBuilder _newCopy() {
    return new FacetsMapBuilder(request, new LinkedHashMap<>(data));
  }

  public FacetsMapBuilder _set(int facetId, FacetValue value) {
    if (this._hasValue(facetId)) {
      throw new IllegalModificationException();
    }
    data.put(facetId, (FacetValue) value);
    return this;
  }
}
