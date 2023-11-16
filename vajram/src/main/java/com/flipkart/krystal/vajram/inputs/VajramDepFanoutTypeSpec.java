package com.flipkart.krystal.vajram.inputs;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.vajram.Vajram;
import java.util.Collection;
import lombok.EqualsAndHashCode;

/**
 * Represents a dependency vajram which can be invoked a variable number of times (fanout) by the
 * current vajram.
 *
 * @param <T> The return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 * @param <DV> The dependency vajram
 */
@EqualsAndHashCode(callSuper = true, cacheStrategy = LAZY)
public final class VajramDepFanoutTypeSpec<T, CV extends Vajram<?>>
    extends VajramDependencySpec<Collection<T>, CV> {

  public VajramDepFanoutTypeSpec(String name, Class<CV> ofVajram) {
    super(name, ofVajram);
  }
}
