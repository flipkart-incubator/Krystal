package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.SingleTargetFanout.SingleTargetFanoutIRStage;

public final class InputResolvers {

  public static <T, CV extends Vajram<?>, DV extends Vajram<?>>
      SingleTargetResolverUsingStage<T, CV, DV> resolve(
          VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> depInput) {
    return new SingleTargetResolverUsingStage<>(dependency, depInput);
  }

  public static <T, CV extends Vajram<?>, DV extends Vajram<?>>
      SingleTargetFanoutIRStage<T, CV, DV> resolveFanout(
          VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> depInputs) {
    return new SingleTargetFanoutIRStage<>(dependency, depInputs);
  }

  private InputResolvers() {}
}
