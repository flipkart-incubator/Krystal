package com.flipkart.krystal.data;

import com.flipkart.krystal.except.IllegalModificationException;
import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("unchecked")
public final class FacetsMapBuilder implements FacetsMap, FacetsBuilder {

  private final SimpleRequestBuilder<Object> request;
  @Getter private ImmutableSet<? extends Facet> _facets;
  private final Map<Integer, FacetValue> otherFacetValues;

  public FacetsMapBuilder(SimpleRequestBuilder<Object> request, Set<? extends Facet> _facets) {
    this(request, _facets, ImmutableMap.of());
  }

  FacetsMapBuilder(
      SimpleRequestBuilder<Object> request,
      Set<? extends Facet> _facets,
      Map<Integer, ? extends FacetValue> otherFacetValues) {
    this.request = request._asBuilder();
    this._facets = ImmutableSet.copyOf(_facets);
    this.otherFacetValues = new LinkedHashMap<>(otherFacetValues);
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
    return otherFacetValues.getOrDefault(facetId, Errable.nil());
  }

  @Override
  public Errable<?> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._get(facetId);
    } else {
      FacetValue datum = otherFacetValues.getOrDefault(facetId, Errable.nil());
      if (datum instanceof Errable<?> errable) {
        return errable;
      } else {
        throw new IllegalArgumentException("%s is not of type Errable".formatted(facetId));
      }
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public FanoutDepResponses _getDepResponses(int facetId) {
    FacetValue datum = otherFacetValues.getOrDefault(facetId, Errable.nil());
    if (datum instanceof FanoutDepResponses fanoutDepResponses) {
      return (FanoutDepResponses) fanoutDepResponses;
    } else if (datum instanceof RequestResponse requestResponse) {
      return new FanoutDepResponses(ImmutableList.of((RequestResponse) requestResponse));
    } else if (datum instanceof One2OneDepResponse.NoRequest noRequest) {
      return new FanoutDepResponses(
          ImmutableList.of(
              new RequestResponse(
                  SimpleImmutRequest.empty(), (Errable<Object>) noRequest.response())));
    } else {
      throw new IllegalArgumentException("%s is not of type Responses".formatted(facetId));
    }
  }

  @Override
  public ImmutableMap<Integer, FacetValue> _asMap() {
    return ImmutableMap.<Integer, FacetValue>builder()
        .putAll(request._asMap())
        .putAll(otherFacetValues)
        .build();
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || otherFacetValues.containsKey(facetId);
  }

  @Override
  public ImmutableFacetsMap _build() {
    return new ImmutableFacetsMap(request, _facets, ImmutableMap.copyOf(otherFacetValues));
  }

  @Override
  public FacetsMapBuilder _newCopy() {
    return new FacetsMapBuilder(request, _facets, new LinkedHashMap<>(otherFacetValues));
  }

  public FacetsMapBuilder _set(int facetId, FacetValue value) {
    if (this._hasValue(facetId)) {
      throw new IllegalModificationException();
    }
    otherFacetValues.put(facetId, (FacetValue) value);
    return this;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (o == this) return true;
    if (!(o instanceof FacetsMap other)) return false;
    return Objects.equals(this._asMap(), other._asMap());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._asMap());
  }
}
