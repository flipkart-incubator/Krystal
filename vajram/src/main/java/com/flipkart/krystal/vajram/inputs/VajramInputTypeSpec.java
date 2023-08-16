package com.flipkart.krystal.vajram.inputs;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.schema.InputTypeSpec;
import com.flipkart.krystal.vajram.Vajram;
import lombok.EqualsAndHashCode;

/**
 * Represents an input of the current vajram. This may also represent a depenedency of this vajram
 * (See: {@link VajramDependencyTypeSpec})
 *
 * @param <T> The data type of the input.
 * @param <V> The current vajram whose input this is.
 */
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
