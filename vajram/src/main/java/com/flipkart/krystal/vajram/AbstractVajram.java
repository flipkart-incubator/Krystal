package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.utils.Vajrams;

abstract sealed class AbstractVajram<T> implements Vajram<T>
    permits NonBlockingVajram, BlockingVajram {

  private String id;

  @Override
  public final String getId() {
    if (id == null) {
      id = Vajrams.getVajramId(getClass()).orElseThrow();
    }
    return id;
  }
}
