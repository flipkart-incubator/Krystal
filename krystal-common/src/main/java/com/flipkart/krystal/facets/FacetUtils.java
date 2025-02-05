package com.flipkart.krystal.facets;

public class FacetUtils {

  public static boolean isGiven(Facet facet) {
    return facet.facetTypes().contains(FacetType.INPUT)
        || facet.facetTypes().contains(FacetType.INJECTION);
  }

  private FacetUtils() {}
}
