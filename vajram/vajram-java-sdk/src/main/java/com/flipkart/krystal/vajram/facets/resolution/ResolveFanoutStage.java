package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatorySingleValueFacetSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalSingleValueFacetSpec;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * The stage which can be used to further specify the fanout resolver of the given targetInput
 *
 * @param <T> The data type of the input being resolved.
 * @param <DV> The dependency whose input is being resolved.
 */
public final class ResolveFanoutStage<T, DV extends Request<?>> {

  private final InputMirrorSpec<T, DV> targetInput;

  ResolveFanoutStage(InputMirrorSpec<T, DV> targetInput) {
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
  public <CV extends Request> SimpleInputResolverSpec<T, CV, DV> usingValuesAsResolver(
      Supplier<? extends Collection<? extends T>> with) {
    return new SimpleInputResolverSpec<>(
        targetInput, null, List.of(), new Transformer.None2Many(with::get));
  }

  public <S, CV extends Request> OptionalSingleValTransformFanoutResolverStage<S, T, CV, DV> using(
      OptionalSingleValueFacetSpec<S, CV> sourceInput) {
    return new OptionalSingleValTransformFanoutResolverStage<>(targetInput, sourceInput);
  }

  public <S, CV extends Request> MandatorySingleValTransformFanoutResolverStage<S, T, CV, DV> using(
      MandatorySingleValueFacetSpec<S, CV> sourceInput) {
    return new MandatorySingleValTransformFanoutResolverStage<>(targetInput, sourceInput);
  }

  public <S, CV extends Request<?>, SDV extends Request<S>>
      MultiValTransformFanoutResolverStage<S, T, CV, DV, SDV> using(
          FanoutDepSpec<S, CV, SDV> sourceInput) {
    return new MultiValTransformFanoutResolverStage<>(targetInput, sourceInput);
  }
}
