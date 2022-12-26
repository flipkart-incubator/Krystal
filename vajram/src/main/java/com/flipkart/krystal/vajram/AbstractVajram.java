package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.Vajrams.getVajramIdString;

abstract sealed class AbstractVajram<T> implements Vajram<T> permits ComputeVajram, IOVajram {

  private VajramID id;

  @Override
  public final VajramID getId() {
    if (id == null) {
      id = new VajramID(getVajramIdString(getClass()).orElseThrow());
    }
    return id;
  }
}
