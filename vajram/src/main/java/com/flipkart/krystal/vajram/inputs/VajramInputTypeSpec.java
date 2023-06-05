package com.flipkart.krystal.vajram.inputs;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.schema.InputTypeSpec;
import com.flipkart.krystal.vajram.Vajram;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(cacheStrategy = LAZY)
public sealed class VajramInputTypeSpec<T, V extends Vajram<?>> implements InputTypeSpec<T>
    permits VajramDependencyTypeSpec {

  private final String name;
  private final Class<V> ofVajram;

  public VajramInputTypeSpec(String name, Class<V> ofVajram) {
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
