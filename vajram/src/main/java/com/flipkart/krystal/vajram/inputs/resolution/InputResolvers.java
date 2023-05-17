package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.FanoutResolverStage.ResolveFanoutStage;
import com.flipkart.krystal.vajram.inputs.resolution.ResolverStage.ResolveStage;

public final class InputResolvers {

  /**
   * Returns a builder stage which can be used to create a simple input resolver (without fanout).
   *
   * @param dependency spec of the dependency being resolved
   * @param depInput spec of the dependency target input being resolved
   * @param <T> Target Type: The DataType of the dependency target input being resolved
   * @param <CV> CurrentVajram: The current vajram which is resolving the input
   * @param <DV> DependencyVajram: The vajram whose input is being resolved
   */
  public static <T, CV extends Vajram<?>, DV extends Vajram<?>> ResolveStage<T, CV, DV> resolve(
      VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> depInput) {
    return new ResolveStage<>(dependency, depInput);
  }

  /**
   * Returns a builder stage which can be used to simple input resolver with fanout.
   *
   * @param dependency spec of the dependency being resolved
   * @param depInput spec of the dependency target input being resolved
   * @param <T> Target Type: The DataType of the dependency target input being resolved
   * @param <CV> CurrentVajram: The current vajram which is resolving the input
   * @param <DV> DependencyVajram: The vajram whose input is being resolved
   */
  public static <T, CV extends Vajram<?>, DV extends Vajram<?>>
      ResolveFanoutStage<T, CV, DV> resolveFanout(
          VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(dependency, depInput);
  }

  private InputResolvers() {}
}
