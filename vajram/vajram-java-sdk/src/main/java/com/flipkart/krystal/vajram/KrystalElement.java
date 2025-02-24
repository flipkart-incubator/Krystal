package com.flipkart.krystal.vajram;

public sealed interface KrystalElement {
  record Vajram() implements KrystalElement {}

  record Facet() implements KrystalElement {}

  record Logic() implements KrystalElement {}
}
