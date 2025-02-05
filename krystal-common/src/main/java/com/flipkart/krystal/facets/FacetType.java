package com.flipkart.krystal.facets;

public enum FacetType {
  /** Facet whose value is provided by client */
  INPUT,
  /** Facet whose value is provided by the runtime */
  INJECTION,
  /** Facet whose value is computed by a dependency */
  DEPENDENCY,
  /** Facet whose value is computed by this vajram */
  OUTPUT
}
