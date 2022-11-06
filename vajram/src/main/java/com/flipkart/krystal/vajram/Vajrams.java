package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.VajramDef;
import java.util.Optional;

public final class Vajrams {

  public static Optional<String> getVajramId(
      @SuppressWarnings("rawtypes") Class<? extends Vajram> aClass) {
    return Optional.ofNullable(aClass.getAnnotation(VajramDef.class)).map(VajramDef::value);
  }

  private Vajrams() {}
}
