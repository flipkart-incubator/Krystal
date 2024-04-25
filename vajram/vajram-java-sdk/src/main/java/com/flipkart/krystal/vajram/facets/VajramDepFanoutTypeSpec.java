package com.flipkart.krystal.vajram.facets;

import static lombok.EqualsAndHashCode.CacheStrategy.LAZY;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.vajram.DependencyResponse;
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
public final class VajramDepFanoutTypeSpec<
        T, CV extends ImmutableRequest<?>, DV extends ImmutableRequest<T>>
    extends VajramDependencySpec<T, DependencyResponse<DV, T>, CV, DV> {

  public VajramDepFanoutTypeSpec(int id, String name, Class<CV> ofVajram, Class<DV> onVajram) {
    super(id, name, ofVajram, onVajram);
  }
}
