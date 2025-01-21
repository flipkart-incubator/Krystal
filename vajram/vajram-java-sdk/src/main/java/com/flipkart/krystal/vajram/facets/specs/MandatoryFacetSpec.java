package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.Request;

/** Spec of a facet which is mandatory */
public sealed interface MandatoryFacetSpec<T, CV extends Request> extends FacetSpec<T, CV>
    permits MandatoryFacetDefaultSpec, MandatoryOne2OneDepSpec {

  @Override
  default boolean isMandatory() {
    return true;
  }

  @Override
  default boolean canFanout() {
    return false;
  }
}
