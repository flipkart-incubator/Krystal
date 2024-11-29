package com.flipkart.krystal.vajram;

import java.lang.annotation.Annotation;

public class Annos {

  public static VajramDef vajramDef(
      VajramDef declaredAnno,
      String inferredVajramId,
      ComputeDelegationType inferredComputeDelegationType) {
    return new VajramDef() {
      @Override
      public String vajramId() {
        return inferredVajramId;
      }

      @Override
      public ComputeDelegationType computeDelegationType() {
        return inferredComputeDelegationType;
      }

      @Override
      public Class<? extends Annotation> annotationType() {
        return VajramDef.class;
      }
    };
  }

  private Annos() {}
}
