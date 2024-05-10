package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.vajram.VajramRequest;

/**
 * Represents a dependency vajram of the current vajram.
 *
 * @param <T> The type of this facet - this is the return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 */
public abstract sealed class VajramDependencySpec<
        T, R, CV extends VajramRequest<?>, DV extends VajramRequest<T>>
    extends VajramFacetSpec<R, CV> permits VajramDepFanoutTypeSpec, VajramDepSingleTypeSpec {

  VajramDependencySpec(String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(name, ofVajram);
  }
}
