package com.flipkart.krystal.krystex.testutils;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.data.One2OneDepResponse;
import com.flipkart.krystal.data.RequestResponse;
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

@SuppressWarnings("unchecked")
public final class FacetValuesMapBuilder implements FacetValuesMap, FacetValuesBuilder {

  private final SimpleRequestBuilder<Object> request;
  @Getter private final ImmutableSet<? extends Facet> _facets;
  private final Map<Integer, FacetValue> otherFacetValues;
  @Getter private final VajramID _vajramID;

  public FacetValuesMapBuilder(
      SimpleRequestBuilder<Object> request, Set<? extends Facet> _facets, VajramID vajramID) {
    this(request, _facets, ImmutableMap.of(), vajramID);
  }

  FacetValuesMapBuilder(
      SimpleRequestBuilder<Object> request,
      Set<? extends Facet> _facets,
      Map<Integer, ? extends FacetValue> otherFacetValues,
      VajramID vajramID) {
    this.request = request._asBuilder();
    this._facets = ImmutableSet.copyOf(_facets);
    this.otherFacetValues = new LinkedHashMap<>(otherFacetValues);
    this._vajramID = vajramID;
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
      return fanoutDepResponses;
    } else if (datum instanceof RequestResponse requestResponse) {
      return new FanoutDepResponses(ImmutableList.of(requestResponse));
    } else if (datum instanceof One2OneDepResponse.NoRequest noRequest) {
      return new FanoutDepResponses(
          ImmutableList.of(
              new RequestResponse(SimpleImmutRequest.empty(_vajramID), noRequest.response())));
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
  public ImmutableFacetValuesMap _build() {
    return new ImmutableFacetValuesMap(
        request, _facets, ImmutableMap.copyOf(otherFacetValues), _vajramID);
  }

  @Override
  public FacetValuesMapBuilder _newCopy() {
    return new FacetValuesMapBuilder(
        request, _facets, new LinkedHashMap<>(otherFacetValues), _vajramID);
  }

  public FacetValuesMapBuilder _set(int facetId, FacetValue value) {
    if (this._hasValue(facetId)) {
      throw new IllegalModificationException();
    }
    otherFacetValues.put(facetId, value);
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this._asMap(), this._vajramID());
  }
}
