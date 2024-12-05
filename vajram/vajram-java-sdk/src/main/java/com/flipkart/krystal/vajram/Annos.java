package com.flipkart.krystal.vajram;

import java.lang.annotation.Annotation;

public class Annos {

  public static VajramDef vajramDef(
      VajramDef declaredAnno,
      String inferredVajramId,
      ComputeDelegationType inferredComputeDelegationType) {
    return new VajramDefImpl(inferredVajramId, inferredComputeDelegationType);
  }

  private Annos() {}

  private record VajramDefImpl(String id, ComputeDelegationType computeDelegationType)
      implements VajramDef {

    @Override
    public Class<? extends Annotation> annotationType() {
      return VajramDef.class;
    }
  }
}
