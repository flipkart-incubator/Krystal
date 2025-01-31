package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.FacetsBuilder;
import com.flipkart.krystal.data.FacetsMap;
import com.flipkart.krystal.data.FacetsMapBuilder;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.SimpleRequest;
import com.flipkart.krystal.data.SimpleRequestBuilder;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.InputMirror;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Include;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.checkerframework.checker.nullness.qual.Nullable;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = PACKAGE)
public @NonFinal sealed class SimpleFacet implements Facet, InputMirror permits SimpleDep {

  public static SimpleFacet input(int facetId) {
    return new SimpleFacet(facetId, "input" + count++, INPUT);
  }

  public static SimpleDep dependency(int facetId) {
    return new SimpleDep(facetId, "dep" + count++);
  }

  private static int count;

  @Include int id;
  String name;

  FacetType facetType;

  @Override
  public ImmutableSet<FacetType> facetTypes() {
    return ImmutableSet.of(facetType);
  }

  @Override
  public @Nullable FacetValue getFacetValue(Facets facets) {
    return ((FacetsMap) facets)._asMap().get(id());
  }

  @Override
  public void setFacetValue(FacetsBuilder facets, FacetValue value) {
    ((FacetsMapBuilder) facets)._set(id(), value);
  }

  @Override
  public String documentation() {
    return facetType + " doc";
  }

  @Override
  public @Nullable Object getFromRequest(Request request) {
    return ((SimpleRequest) request)._asMap().get(id());
  }

  @Override
  public void setToRequest(Builder request, @Nullable Object value) {
    ((SimpleRequestBuilder<Object>) request)._asMap().put(id(), Errable.withValue(value));
  }
}
