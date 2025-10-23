package com.flipkart.krystal.krystex.testfixtures;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INPUT;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.FacetValuesBuilder;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.facets.InputMirror;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public sealed class SimpleFacet implements Facet, InputMirror permits SimpleDep {

  public static SimpleFacet input(int facetId) {
    return new SimpleFacet(facetId, "input" + count++, INPUT);
  }

  public static SimpleDep dependency(int facetId, VajramID ofVajramID, VajramID onVajramID) {
    return new SimpleDep(facetId, "dep" + count++, ofVajramID, onVajramID);
  }

  private static int count;

  private final int id;
  private final String name;

  private final FacetType facetType;

  public SimpleFacet(int id, String name, FacetType facetType) {
    this.id = id;
    this.name = name;
    this.facetType = facetType;
  }

  public FacetType facetType() {
    return facetType;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public @Nullable FacetValue getFacetValue(FacetValues facetValues) {
    return ((FacetValuesMap) facetValues)._asMap().get(id());
  }

  @Override
  public void setFacetValue(FacetValuesBuilder facets, FacetValue value) {
    ((FacetValuesMapBuilder) facets)._set(id(), value);
  }

  @Override
  public String documentation() {
    return facetType + " doc";
  }

  @Override
  public @Nullable Object getFromRequest(Request request) {
    return ((SimpleRequest) request)._asMap().get(id());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setToRequest(Builder request, @Nullable Object value) {
    ((SimpleRequestBuilder<Object>) request)._asMap().put(id(), Errable.withValue(value));
  }

  @Override
  public DataType<?> type() {
    return JavaType.create(Object.class);
  }

  @Override
  public VajramID ofVajramID() {
    return vajramID("testVajram");
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    SimpleFacet that = (SimpleFacet) o;
    return id == that.id && facetType == that.facetType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, facetType);
  }
}
