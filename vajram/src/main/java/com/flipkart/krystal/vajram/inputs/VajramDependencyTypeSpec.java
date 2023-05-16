package com.flipkart.krystal.vajram.inputs;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.vajram.Vajram;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true, cacheStrategy = LAZY)
public final class VajramDependencyTypeSpec<T, CV extends Vajram<?>, DV extends Vajram<T>>
    extends VajramInputTypeSpec<T, CV> {

  private final Class<DV> onVajram;

  public VajramDependencyTypeSpec(String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(name, ofVajram);
    this.onVajram = onVajram;
  }

  public Class<DV> onVajram() {
    return onVajram;
  }
}
