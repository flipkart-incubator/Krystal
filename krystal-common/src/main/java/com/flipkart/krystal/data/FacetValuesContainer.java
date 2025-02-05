package com.flipkart.krystal.data;

import com.flipkart.krystal.facets.BasicFacetInfo;
import com.google.common.collect.ImmutableSet;

/**
 * Marker interface extended by auto-generated classes holding facet values of a vajram. This class
 * is intended for use by the krystal platform code generator. Developers must not implement/extend
 * this interface - this can lead to unexpected behaviour.
 */
public interface FacetValuesContainer {

  /** Returns the basic facet infos of the facets whose values this container contains */
  ImmutableSet<? extends BasicFacetInfo> _facets();
}
