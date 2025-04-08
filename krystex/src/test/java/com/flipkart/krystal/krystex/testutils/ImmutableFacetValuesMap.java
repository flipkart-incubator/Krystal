package com.flipkart.krystal.krystex.testutils;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ImmutableFacetValuesMap implements FacetValuesMap, ImmutableFacetValues {

  private final SimpleImmutRequest<Object> request;
  @Getter private ImmutableSet<? extends Facet> _facets;
  private final ImmutableMap<Integer, FacetValue> otherFacets;

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> request, Set<? extends Facet> _facets) {
    this(request, _facets, ImmutableMap.of());
  }

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> request,
      Set<? extends Facet> _facets,
      ImmutableMap<Integer, FacetValue> otherFacets) {
    this.request = request._build();
    this._facets = ImmutableSet.copyOf(_facets);
    this.otherFacets = otherFacets;
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
    return otherFacets.getOrDefault(facetId, Errable.nil());
  }

  @Override
  public Errable<?> _getErrable(int facetId) {
    if (request._hasValue(facetId)) {
      return request._get(facetId);
    } else {
      FacetValue datum = otherFacets.getOrDefault(facetId, Errable.nil());
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
    FacetValue datum = otherFacets.getOrDefault(facetId, Errable.nil());
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
        .putAll(otherFacets)
        .build();
  }

  public boolean _hasValue(int facetId) {
    return request._hasValue(facetId) || otherFacets.containsKey(facetId);
  }

  @Override
  public FacetValuesMapBuilder _asBuilder() {
    return new FacetValuesMapBuilder(request._asBuilder(), _facets, otherFacets);
  }

  @Override
  public ImmutableFacetValuesMap _build() {
    return this;
  }

  @Override
  public ImmutableFacetValuesMap _newCopy() {
    return this;
  }

  @Override
  public boolean equals(final @Nullable Object o) {
    if (o == this) return true;
    if (!(o instanceof FacetValuesMap other)) return false;
    return Objects.equals(this._asMap(), other._asMap());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._asMap());
  }
}
