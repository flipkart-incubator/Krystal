package com.flipkart.krystal.vajram;

import java.util.Optional;

public final class Vajrams {

  public static Optional<String> getVajramIdString(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    return Optional.ofNullable(aClass.getAnnotation(VajramDef.class)).map(VajramDef::value);
  }

  private Vajrams() {}
}
