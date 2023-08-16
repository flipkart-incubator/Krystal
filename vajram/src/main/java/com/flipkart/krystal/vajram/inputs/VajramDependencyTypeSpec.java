package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.Vajram;

/**
 * Represents a dependency vajram of the current vajram.
 *
 * @param <T> The return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 * @param <DV> The dependency vajram
 */
public abstract sealed class VajramDependencyTypeSpec<
        T, P, CV extends Vajram<?>, DV extends Vajram<T>>
    extends VajramInputTypeSpec<P, CV> permits VajramDepFanoutTypeSpec, VajramDepSingleTypeSpec {

  private final Class<DV> onVajram;

  VajramDependencyTypeSpec(String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(name, ofVajram);
    this.onVajram = onVajram;
  }

  public Class<DV> onVajram() {
    return onVajram;
  }
}
