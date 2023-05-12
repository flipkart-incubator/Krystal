package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.schema.InputDefinition;
import com.flipkart.krystal.vajram.Vajram;

public sealed class VajramInput<T, V extends Vajram<?>> implements InputDefinition<T>
    permits VajramDependency {

  private final String name;
  private final Class<V> ofVajram;

  public VajramInput(String name, Class<V> ofVajram) {
    this.name = name;
    this.ofVajram = ofVajram;
  }

  @Override
  public String name() {
    return name;
  }

  public Class<V> ofVajram() {
    return ofVajram;
  }
}
