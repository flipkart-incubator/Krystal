package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.specs.FanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.flipkart.krystal.vajram.facets.specs.MandatorySingleValueFacetSpec;
import com.flipkart.krystal.vajram.facets.specs.OptionalSingleValueFacetSpec;
import java.util.List;
import java.util.function.Supplier;

/**
 * The stage which can be used to further specify the non-fanout resolver of the given targetInput
 *
 * @param <T> The data type of the target input being resolved.
 * @param <DV> The dependency whose input is being resolved.
 * @param <R> The data type of the dependency output
 */
public final class ResolveStage<T, R, DV extends Request<R>> {
  private final InputMirrorSpec<T, DV> targetInput;

  ResolveStage(InputMirrorSpec<T, DV> targetInput) {
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
  public <CV extends Request> SimpleInputResolverSpec<T, CV, DV> usingValueAsResolver(
      Supplier<T> with) {
    return new SimpleInputResolverSpec<>(
        targetInput, null, List.of(), new Transformer.None2One(with::get));
  }

  /**
   * Use the value of the source input as-is, without any transformation. If the source input is
   * {@link Errable#nil()}, then the resolved value will also be {@link Errable#nil()}. This is
   * possible only if the dataType of the sourceInput and the target input are same.
   *
   * @see #using(OptionalSingleValueFacetSpec)
   * @see #using(MandatorySingleValueFacetSpec)
   * @see #using(FanoutDepSpec)
   * @param sourceInput the spec of the source input being used for resolution
   */
  public <CV extends Request> OptionalAsIsResolverStage<T, CV, DV> usingAsIs(
      OptionalSingleValueFacetSpec<T, CV> sourceInput) {
    return new OptionalAsIsResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input as-is, without any transformation. This is possible only if
   * the dataType of the sourceInput and the target input are same.
   *
   * @see #using(OptionalSingleValueFacetSpec)
   * @see #using(MandatorySingleValueFacetSpec)
   * @see #using(FanoutDepSpec)
   * @param sourceInput the spec of the source input being used for resolution
   */
  public <CV extends Request> MandatoryAsIsResolverStage<T, CV, DV> usingAsIs(
      MandatorySingleValueFacetSpec<T, CV> sourceInput) {
    return new MandatoryAsIsResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S, CV extends Request> OptionalSingleValTransformResolverStage<S, T, CV, DV> using(
      OptionalSingleValueFacetSpec<S, CV> sourceInput) {
    return new OptionalSingleValTransformResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S, CV extends Request> MandatorySingleValTransformResolverStage<S, T, CV, DV> using(
      MandatorySingleValueFacetSpec<S, CV> sourceInput) {
    return new MandatorySingleValTransformResolverStage<>(targetInput, sourceInput);
  }

  /**
   * Use the value of the source input and transform it to compute the resolved value.
   *
   * @param sourceInput the spec of the source input whose value is used to resolve the dependency
   *     input.
   */
  public <S, CV extends Request<?>> MultiValTransformResolverStage<S, T, CV, R, DV> using(
      FanoutDepSpec<S, CV, ?> sourceInput) {
    return new MultiValTransformResolverStage<>(targetInput, sourceInput);
  }
}
