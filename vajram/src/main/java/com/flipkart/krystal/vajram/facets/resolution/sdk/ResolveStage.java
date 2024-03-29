package com.flipkart.krystal.vajram.facets.resolution.sdk;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.flipkart.krystal.vajram.facets.resolution.SimpleInputResolverSpec;
import java.util.List;
import java.util.function.Supplier;

/**
 * The stage which can be used to further specify the non-fanout resolver of the given targetInput
 *
 * @param <I> The data type of the input being resolved.
 * @param <DV> The dependency whose input is being resolved.
 */
public final class ResolveStage<I, DV extends VajramRequest<?>> {
  private final VajramFacetSpec<I, DV> targetInput;

  ResolveStage(VajramFacetSpec<I, DV> targetInput) {
    this.targetInput = targetInput;
  }

  /**
   * Use the value of the source input as-is, without any transformation. If the source input is
   * {@link Errable#empty()}, then the resolved value will also be {@link Errable#empty()}. This is
   * possible only if the dataType of the sourceInput and the target input are same.
   *
   * @see #using(VajramFacetSpec)
   * @param sourceInput the spec of the source input being used for resolution
   */
  public <CV extends VajramRequest<?>> AsIsResolverStage<I, CV, DV> usingAsIs(
      VajramFacetSpec<I, CV> sourceInput) {
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
  public <CV extends VajramRequest<?>> SimpleInputResolverSpec<I, CV, DV> usingValueAsResolver(
      Supplier<I> with) {
    return new SimpleInputResolverSpec<>(targetInput, List.of(), List.of(), o -> with.get(), null);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S, CV extends VajramRequest<?>> Transform1ResolverStage<S, I, CV, DV> using(
      VajramFacetSpec<S, CV> sourceInput) {
    return new Transform1ResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput1 the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S1, S2, CV extends VajramRequest<?>> Transform2ResolverStage<S1, S2, I, CV, DV> using(
      VajramFacetSpec<S1, CV> sourceInput1, VajramFacetSpec<S2, CV> sourceInput2) {
    return new Transform2ResolverStage<>(targetInput, sourceInput1, sourceInput2);
  }
}
