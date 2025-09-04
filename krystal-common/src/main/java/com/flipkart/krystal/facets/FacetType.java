package com.flipkart.krystal.facets;

import lombok.Getter;

@Getter
public enum FacetType {
  /** Facet whose value is provided by client */
  INPUT(false, true),
  /** Facet whose value is provided by the runtime */
  INJECTION(true, true),
  /** Facet whose value is computed by a dependency */
  DEPENDENCY(true, false),
  /** Facet whose value is computed by this vajram */
  OUTPUT(true, false);

  /** true if this facet is not part of the client facing contract of the vajram */
  private final boolean isInternal;

  /** true if this facet's value is provided by the client or the runtime */
  private final boolean isGiven;

  FacetType(boolean isInternal, boolean isGiven) {
    this.isInternal = isInternal;
    this.isGiven = isGiven;
  }

  /**
   * Returns true if this facet's value is computed by a dependency or this vajram and is not
   * provided from outside the vajram.
   */
  public boolean isComputed() {
    return !isGiven;
  }

  /** Returns true if this facet is part of the client facing contract of the vajram. */
  public boolean isExternal() {
    return !isInternal;
  }
}
