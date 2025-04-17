package com.flipkart.krystal.core;

public sealed interface KrystalElement {
  sealed interface VajramRoot extends KrystalElement {}

  record Vajram() implements VajramRoot {}

  record Trait() implements VajramRoot {}

  sealed interface Facet extends KrystalElement {
    record Dependency() implements Facet {}
  }

  record Logic() implements KrystalElement {}
}
