package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;

/**
 * Represents a dependency vajram of the current vajram.
 *
 * @param <T> The type of this facet - this is the return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 */
public abstract sealed class VajramDependencySpec<
        T, R, CV extends Request<?>, DV extends Request<T>>
    extends VajramFacetSpec<R, CV> permits VajramDepFanoutTypeSpec, VajramDepSingleTypeSpec {

  VajramDependencySpec(int id, String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(id, name, ofVajram);
  }
}
