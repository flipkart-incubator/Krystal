package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.Vajram;

/**
 * Represents a dependency vajram of the current vajram.
 *
 * @param <T> The type of this facet
 * @param <CV> The current vajram which has the dependency
 */
public abstract sealed class VajramDependencySpec<T, CV extends Vajram<?>>
    extends VajramFacetSpec<T> permits VajramDepFanoutTypeSpec, VajramDepSingleTypeSpec {

  private final Class<CV> ofVajram;

  VajramDependencySpec(String name, Class<CV> ofVajram) {
    super(name);
    this.ofVajram = ofVajram;
  }
}
