package com.flipkart.krystal.krystex.testfixtures;

import static com.flipkart.krystal.core.VajramID.vajramID;
import static com.flipkart.krystal.facets.FacetType.INPUT;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ErrableFacetValue;
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

  public static SimpleFacet input(String name) {
    return new SimpleFacet(name, INPUT);
  }

  public static SimpleDep dependency(String name, VajramID ofVajramID, VajramID onVajramID) {
    return new SimpleDep(name, ofVajramID, onVajramID);
  }

  private final String name;

  private final FacetType facetType;

  public SimpleFacet(String name, FacetType facetType) {
    this.name = name;
    this.facetType = facetType;
  }

  public FacetType facetType() {
    return facetType;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public @Nullable FacetValue getFacetValue(FacetValues facetValues) {
    return ((FacetValuesMap) facetValues)._asMap().get(name());
  }

  @Override
  public void setFacetValue(FacetValuesBuilder facets, FacetValue value) {
    ((FacetValuesMapBuilder) facets)._set(name(), value);
  }

  @Override
  public String documentation() {
    return facetType + " doc";
  }

  @Override
  public @Nullable Object getFromRequest(Request request) {
    return ((SimpleRequest) request)._asMap().get(name());
  }

  @SuppressWarnings("unchecked")
  @Override
  public void setToRequest(Builder request, @Nullable Object value) {
    ((SimpleRequestBuilder<Object>) request)
        ._asMap()
        .put(name(), new ErrableFacetValue<>(Errable.withValue(value)));
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
    return name().equals(that.name()) && facetType == that.facetType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, facetType);
  }
}
