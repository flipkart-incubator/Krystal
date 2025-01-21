package com.flipkart.krystal.krystex.testutils;

import static com.flipkart.krystal.facets.FacetType.INPUT;
import static lombok.AccessLevel.PACKAGE;

import com.flipkart.krystal.facets.Facet;
import com.flipkart.krystal.facets.FacetType;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = PACKAGE)
public @NonFinal sealed class SimpleFacet implements Facet permits SimpleDep {

  public static SimpleFacet input(int facetId) {
    return new SimpleFacet(facetId, "input" + count++, INPUT);
  }

  public static SimpleDep dependency(int facetId) {
    return new SimpleDep(facetId, "dep" + count++);
  }

  private static int count;

  @EqualsAndHashCode.Include int id;
  String name;

  FacetType facetType;

  @Override
  public ImmutableSet<FacetType> facetTypes() {
    return ImmutableSet.of(facetType);
  }

  @Override
  public String documentation() {
    return facetType + " doc";
  }
}
