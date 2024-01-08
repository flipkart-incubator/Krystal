package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.toResolver;
import static java.util.Arrays.stream;

import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.FanoutResolverStage.ResolveFanoutStage;
import com.flipkart.krystal.vajram.facets.resolution.ResolverStage.ResolveStage;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public final class InputResolvers {

  /**
   * @param inputResolvers Array of lists of resolvers where each list contains resolvers for one
   *     dependency
   * @return An aggregated list of all the resolvers
   */
  @SafeVarargs
  public static ImmutableList<InputResolver> resolve(List<InputResolver>... inputResolvers) {
    return stream(inputResolvers)
        .flatMap(Collection::stream)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * @param dependency The dependency whose inputs are being resolved
   * @param resolverStages The resolver specs of the dependency
   * @return The list of InputResolvers
   * @param <T> The return type of the dependency.
   * @param <CV> The current vajram which has the dependency
   * @param <DV> The dependency vajram
   */
  @SafeVarargs
  public static <T, CV extends VajramRequest<?>, DV extends VajramRequest<T>>
      List<InputResolver> dep(
          VajramDependencySpec<T, CV, DV> dependency,
          SimpleInputResolverSpec<?, ?, CV, DV>... resolverStages) {
    return stream(resolverStages)
        .map(
            spec -> {
              return toResolver(dependency, spec);
            })
        .toList();
  }

  /**
   * @param depInput The input which is being resolved with a single value (no fanout)
   * @return A {@link ResolveStage} which can be used to further specify the details of the resolver
   * @param <T> The data type of the input
   * @param <DV> The dependency whose input is being resolved.
   */
  public static <T, DV extends VajramRequest<?>> ResolveStage<T, DV> depInput(
      VajramFacetSpec<T, DV> depInput) {
    return new ResolveStage<>(depInput);
  }

  /**
   * @param depInput The input which is being resolved with a variable number of values (fanout)
   * @return A {@link ResolveFanoutStage} which can be used to further specify the details of the
   *     fanout resolver
   * @param <T> The data type of the input being resolved.
   * @param <DV> The dependency whose input is being resolved.
   */
  public static <T, DV extends VajramRequest<?>> ResolveFanoutStage<T, DV> fanout(
      VajramFacetSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(depInput);
  }

  /**
   * Returns a builder stage which can be used to simple input resolver with fanout.
   *
   * @param depInput spec of the dependency target input being resolved
   * @param <T> Target Type: The DataType of the dependency target input being resolved
   * @param <DV> DependencyVajram: The vajram whose input is being resolved
   */
  public static <T, DV extends VajramRequest<?>> ResolveFanoutStage<T, DV> resolveFanout(
      VajramFacetSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(depInput);
  }

  private InputResolvers() {}
}
