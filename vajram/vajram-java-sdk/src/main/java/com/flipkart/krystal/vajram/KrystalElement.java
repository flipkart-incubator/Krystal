package com.flipkart.krystal.vajram;

public sealed interface KrystalElement {
  public sealed interface VajramRoot extends KrystalElement {}

  record Vajram() implements VajramRoot {}

  record VajramTrait() implements VajramRoot {}

  record Facet() implements KrystalElement {}

  record Logic() implements KrystalElement {}
}
