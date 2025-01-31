package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * The stage which can be used to further specify the non-fanout resolver of the given targetInput
 *
 * @param <I> The data type of the input being resolved.
 * @param <DV> The dependency whose input is being resolved.
 */
public final class ResolveStage<I, DV extends Request> {
  private final InputMirrorSpec<I, DV> targetInput;

  ResolveStage(InputMirrorSpec<I, DV> targetInput) {
    this.targetInput = targetInput;
  }

  /**
   * Use the value of the source input as-is, without any transformation. If the source input is
   * {@link Errable#nil()}, then the resolved value will also be {@link Errable#nil()}. This is
   * possible only if the dataType of the sourceInput and the target input are same.
   *
   * @see #using(FacetSpec)
   * @param sourceInput the spec of the source input being used for resolution
   */
  public <CV extends Request> AsIsResolverStage<I, CV, DV> usingAsIs(FacetSpec<I, CV> sourceInput) {
    return new AsIsResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Creates a resolver spec which does not have any source. This is useful when we want to
   * statically bind a value to a dependency input
   *
   * @param with a supplier which provides the value which is used to resolve the dependency input
   * @return The resultant {@link SimpleInputResolverSpec}
   * @param <CV> The current vajram which is doing the resolution
   */
  public <CV extends Request> SimpleInputResolverSpec<I, CV, DV> usingValueAsResolver(
      Supplier<I> with) {
    return new SimpleInputResolverSpec<>(
        targetInput, ImmutableSet.of(), List.of(), o -> with.get(), null);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S, CV extends Request> Transform1ResolverStage<S, I, CV, DV> using(
      FacetSpec<S, CV> sourceInput) {
    return new Transform1ResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput1 the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S1, S2, CV extends Request> Transform2ResolverStage<S1, S2, I, CV, DV> using(
      FacetSpec<S1, CV> sourceInput1, FacetSpec<S2, CV> sourceInput2) {
    return new Transform2ResolverStage<>(targetInput, sourceInput1, sourceInput2);
  }
}
