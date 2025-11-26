package com.flipkart.krystal.krystex.testfixtures;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ErrableFacetValue;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.facets.Facet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ImmutableFacetValuesMap implements FacetValuesMap, ImmutableFacetValues {

  private final ImmutableSet<? extends Facet> _facets;
  private final VajramID _vajramID;

  private final SimpleImmutRequest<Object> _request;
  private final ImmutableMap<Integer, FacetValue> otherFacetValues;

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> _request, Set<? extends Facet> _facets, VajramID vajramID) {
    this(_request, _facets, ImmutableMap.of(), vajramID);
  }

  public ImmutableFacetValuesMap(
      SimpleRequestBuilder<Object> _request,
      Set<? extends Facet> _facets,
      ImmutableMap<Integer, FacetValue> otherFacetValues,
      VajramID vajramID) {
    this._request = _request._build();
    this._facets = ImmutableSet.copyOf(_facets);
    this.otherFacetValues = otherFacetValues;
    this._vajramID = vajramID;
  }

  public FacetValue _get(int facetId) {
    if (_request._hasValue(facetId)) {
      ErrableFacetValue<Object> v = _request._get(facetId);
      if (v != null) {
        return v;
      } else {
        throw new AssertionError("This should not be possible since _hasValue is true");
      }
    }
    return otherFacetValues.getOrDefault(facetId, ErrableFacetValue.nil());
  }

  @Override
  public Errable<?> _getOne2OneResponse(int facetId) {
    if (_request._hasValue(facetId)) {
      return _request._get(facetId).asErrable();
    } else {
      FacetValue datum = otherFacetValues.getOrDefault(facetId, ErrableFacetValue.nil());
      if (datum instanceof One2OneDepResponse<?, ?> one2OneDepResponse) {
        return one2OneDepResponse.response();
      } else {
        throw new IllegalArgumentException(
            "%s is not of type RequestResponse. It is of type %s"
                .formatted(facetId, datum.getClass()));
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public FanoutDepResponses _getDepResponses(int facetId) {
    FacetValue datum = otherFacetValues.getOrDefault(facetId, ErrableFacetValue.nil());
    if (datum instanceof FanoutDepResponses errable) {
      return errable;
    } else {
      throw new IllegalArgumentException(
          "%s is not of type FanoutDepResponses.".formatted(facetId));
    }
  }

  @Override
  public ImmutableMap<Integer, FacetValue> _asMap() {
    return ImmutableMap.<Integer, FacetValue>builder()
        .putAll(_request._asMap())
        .putAll(otherFacetValues)
        .build();
  }

  public boolean _hasValue(int facetId) {
    return _request._hasValue(facetId) || otherFacetValues.containsKey(facetId);
  }

  @Override
  public FacetValuesMapBuilder _asBuilder() {
    return new FacetValuesMapBuilder(_request._asBuilder(), _facets, otherFacetValues, _vajramID);
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

  @Override
  public SimpleImmutRequest<Object> _request() {
    return _request;
  }

  @Override
  public ImmutableSet<? extends Facet> _facets() {
    return _facets;
  }

  @Override
  public VajramID _vajramID() {
    return _vajramID;
  }
}
