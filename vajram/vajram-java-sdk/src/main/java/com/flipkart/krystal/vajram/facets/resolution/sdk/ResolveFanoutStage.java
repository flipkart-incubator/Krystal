package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * The stage which can be used to further specify the fanout resolver of the given targetInput
 *
 * @param <I> The data type of the input being resolved.
 * @param <DV> The dependency whose input is being resolved.
 */
public final class ResolveFanoutStage<I, DV extends Request> {

  private final InputMirrorSpec<I, DV> targetInput;

  ResolveFanoutStage(InputMirrorSpec<I, DV> targetInput) {
    this.targetInput = targetInput;
  }

  /**
   * Creates a resolver spec which does not have any source. This is useful when we want to
   * statically bind a value to a dependency input
   *
   * @param with a supplier which provides the value which is used to resolve the dependency input
   * @return The resultant {@link SimpleInputResolverSpec}
   * @param <CV> The current vajram which is doing the resolution
   */
  public <CV extends Request> SimpleInputResolverSpec<I, CV, DV> usingValuesAsResolver(
      Supplier<? extends Collection<? extends I>> with) {
    return new SimpleInputResolverSpec<>(
        targetInput, ImmutableSet.of(), List.of(), null, o -> with.get());
  }

  public <S, CV extends Request> Transform1FanoutResolverStage<S, I, CV, DV> using(
      FacetSpec<S, CV> sourceInput) {
    return new Transform1FanoutResolverStage<>(targetInput, sourceInput);
  }

  public <S1, S2, CV extends Request> Transform2FanoutResolverStage<S1, S2, I, CV, DV> using(
      FacetSpec<S1, CV> sourceInput1, FacetSpec<S2, CV> sourceInput2) {
    return new Transform2FanoutResolverStage<>(targetInput, sourceInput1, sourceInput2);
  }
}
