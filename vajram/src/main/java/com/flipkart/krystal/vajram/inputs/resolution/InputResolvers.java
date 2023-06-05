package com.flipkart.krystal.vajram.inputs.resolution;

import static java.util.Arrays.stream;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.FanoutResolverStage.ResolveFanoutStage;
import com.flipkart.krystal.vajram.inputs.resolution.ResolverStage.ResolveStage;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public final class InputResolvers {

  @SafeVarargs
  public static ImmutableList<InputResolver> resolve(List<InputResolver>... inputResolvers) {
    return stream(inputResolvers)
        .flatMap(Collection::stream)
        .collect(ImmutableList.toImmutableList());
  }

  @SafeVarargs
  public static <T, CV extends Vajram<?>, DV extends Vajram<T>> List<InputResolver> dep(
      VajramDependencyTypeSpec<T, CV, DV> dependency,
      InputResolverSpec<?, ?, CV, DV>... resolverStages) {
    return stream(resolverStages)
        .map(
            spec -> {
              return InputResolverUtil.toResolver(dependency, spec);
            })
        .toList();
  }

  public static <T, DV extends Vajram<?>> ResolveStage<T, DV> depInput(
      VajramInputTypeSpec<T, DV> depInput) {
    return new ResolveStage<>(depInput);
  }

  public static <T, DV extends Vajram<?>> ResolveFanoutStage<T, DV> fanout(
      VajramInputTypeSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(depInput);
  }

  /**
   * Returns a builder stage which can be used to simple input resolver with fanout.
   *
   * @param depInput spec of the dependency target input being resolved
   * @param <T> Target Type: The DataType of the dependency target input being resolved
   * @param <CV> CurrentVajram: The current vajram which is resolving the input
   * @param <DV> DependencyVajram: The vajram whose input is being resolved
   */
  public static <T, CV extends Vajram<?>, DV extends Vajram<?>>
      ResolveFanoutStage<T, DV> resolveFanout(VajramInputTypeSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(depInput);
  }

  private InputResolvers() {}
}
