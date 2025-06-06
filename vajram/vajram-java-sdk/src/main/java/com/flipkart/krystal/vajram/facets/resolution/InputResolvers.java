package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.resolution.InputResolverUtil.toResolver;
import static java.util.Arrays.stream;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableList;
import java.util.List;

public final class InputResolvers {

  /**
   * Starting point for the resolution DSL
   *
   * @param inputResolvers Array of lists of resolvers where each list contains resolvers for one
   *     dependency
   * @return An aggregated list of all the resolvers
   */
  @SafeVarargs
  public static ImmutableList<SimpleInputResolver> resolve(
      List<? extends SimpleInputResolver>... inputResolvers) {
    ImmutableList.Builder<SimpleInputResolver> builder = ImmutableList.builder();
    for (List<? extends SimpleInputResolver> inputResolver : inputResolvers) {
      builder.addAll(inputResolver);
    }
    return builder.build();
  }

  /**
   * Starting point for defining the resolvers of a dependency
   *
   * @param dependency The dependency whose inputs are being resolved
   * @param resolverStages The resolver specs of the dependency
   * @param <T> The return type of the dependency.
   * @param <CV> The current vajram which has the dependency
   * @param <DV> The dependency vajram
   * @return The list of InputResolvers
   */
  @SafeVarargs
  public static <T, CV extends Request, DV extends Request<T>>
      List<? extends SimpleInputResolver> dep(
          DependencySpec<T, CV, DV> dependency,
          SimpleInputResolverSpec<?, CV, DV>... resolverStages) {
    return stream(resolverStages).map(spec -> toResolver(dependency, spec)).toList();
  }

  /**
   * Specify the (one-to-one) dependency input which is being resolved.
   *
   * @param depInput The input which is being resolved with a single value (no fanout)
   * @return A {@link ResolveStage} which can be used to further specify the details of the resolver
   * @param <T> The data type of the input
   * @param <DV> The dependency whose input is being resolved.
   */
  public static <I, T, DV extends Request<T>> ResolveStage<I, T, DV> depInput(
      InputMirrorSpec<I, DV> depInput) {
    return new ResolveStage<>(depInput);
  }

  /**
   * Specify the (fanout) dependency input which is being resolved.
   *
   * @param depInput The input which is being resolved with a variable number of values (fanout)
   * @return A {@link ResolveFanoutStage} which can be used to further specify the details of the
   *     fanout resolver
   * @param <T> The data type of the input being resolved.
   * @param <DV> The dependency whose input is being resolved.
   */
  public static <T, DV extends Request<?>> ResolveFanoutStage<T, DV> depInputFanout(
      InputMirrorSpec<T, DV> depInput) {
    return new ResolveFanoutStage<>(depInput);
  }

  private InputResolvers() {}
}
