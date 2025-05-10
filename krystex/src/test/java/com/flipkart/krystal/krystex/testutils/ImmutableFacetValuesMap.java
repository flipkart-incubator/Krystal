package com.flipkart.krystal.krystex.testutils;

import com.flipkart.krystal.core.VajramID;
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

  @Getter private final ImmutableSet<? extends Facet> _facets;
  @Getter private final VajramID _vajramID;

  private final SimpleImmutRequest<Object> request;
  private final ImmutableMap<Integer, FacetValue> otherFacetValues;

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> request, Set<? extends Facet> _facets, VajramID vajramID) {
    this(request, _facets, ImmutableMap.of(), vajramID);
  }

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> request,
      Set<? extends Facet> _facets,
      ImmutableMap<Integer, FacetValue> otherFacetValues,
      VajramID vajramID) {
    this.request = request._build();
    this._facets = ImmutableSet.copyOf(_facets);
    this.otherFacetValues = otherFacetValues;
    this._vajramID = vajramID;
  }

  public FacetValue _get(int facetId) {
    if (request._hasValue(facetId)) {
      Errable<Object> v = request._get(facetId);
      if (v != null) {
        return v;
      } else {
        throw new AssertionError("This should not be possible since _hasValue is true");
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

  @SuppressWarnings("unchecked")
  @Override
  public FanoutDepResponses _getDepResponses(int facetId) {
    FacetValue datum = otherFacetValues.getOrDefault(facetId, Errable.nil());
    if (datum instanceof FanoutDepResponses errable) {
      return errable;
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
  public FacetValuesMapBuilder _asBuilder() {
    return new FacetValuesMapBuilder(request._asBuilder(), _facets, otherFacetValues, _vajramID);
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
    if (!(o instanceof ImmutableFacetValuesMap other)) return false;
    return Objects.equals(this._asMap(), other._asMap())
        && Objects.equals(this._vajramID(), other._vajramID());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._asMap(), this._vajramID());
  }
}
