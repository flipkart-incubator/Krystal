package com.flipkart.krystal.vajram.facets.specs;

import com.flipkart.krystal.data.Request;

/** Spec of a facet which is optional */
public sealed interface OptionalFacetSpec<T, CV extends Request> extends FacetSpec<T, CV>
    permits FanoutDepSpec, OptionalFacetDefaultSpec, OptionalOne2OneDepSpec {

  @Override
  default boolean isMandatory() {
    return false;
  }

  @Override
  default boolean canFanout() {
    return false;
  }
}
