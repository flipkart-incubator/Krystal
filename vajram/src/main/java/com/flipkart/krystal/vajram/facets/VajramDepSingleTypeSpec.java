package com.flipkart.krystal.vajram.facets;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.vajram.Vajram;
import lombok.EqualsAndHashCode;

/**
 * Represents a dependency vajram which can be invoked exactly once (no fanout) by the current
 * vajram.
 *
 * @param <T> The return type of the dependency vajram
 * @param <CV> The current vajram which has the dependency
 * @param <DVR> The dependency vajram
 */
@EqualsAndHashCode(callSuper = true, cacheStrategy = LAZY)
public final class VajramDepSingleTypeSpec<T, CV extends Vajram<?>>
    extends VajramDependencySpec<T, CV> {

  public VajramDepSingleTypeSpec(String name, Class<CV> ofVajram) {
    super(name, ofVajram);
  }
}
