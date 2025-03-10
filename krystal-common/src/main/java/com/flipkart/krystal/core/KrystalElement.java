package com.flipkart.krystal.core;

public sealed interface KrystalElement {
  public sealed interface VajramRoot extends KrystalElement {}

  record Vajram() implements VajramRoot {}

  record Trait() implements VajramRoot {}

  record Facet() implements KrystalElement {}

  record Logic() implements KrystalElement {}
}
