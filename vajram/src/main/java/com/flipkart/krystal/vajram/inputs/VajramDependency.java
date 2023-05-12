package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.vajram.Vajram;

public final class VajramDependency<T, CV extends Vajram<?>, DV extends Vajram<T>>
    extends VajramInput<T, CV> {

  private final Class<DV> onVajram;

  public VajramDependency(String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(name, ofVajram);
    this.onVajram = onVajram;
  }

  public Class<DV> onVajram() {
    return onVajram;
  }
}
