package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public final class FanoutResolverStage<S, T, CV extends Vajram<?>, DV extends Vajram<?>> {
  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

  FanoutResolverStage(
      VajramInputTypeSpec<T, DV> targetInput, VajramInputTypeSpec<S, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  /**
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  public FanoutResolverStage<S, T, CV, DV> skipIf(
      Predicate<Optional<S>> whenToSkip, String reason) {
    this.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
    return this;
  }

  /**
   * @param transformer The logic to use to tranform the source data type {@code S} and fanout to a
   *     collection of the target data type {@code T}
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  public SimpleInputResolverSpec<S, T, CV, DV> with(
      Function<Optional<S>, ? extends Collection<? extends T>> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput, sourceInput, skipConditions, null, transformer);
  }

  /**
   * The stage which can be used to further specify the fanout resolver of the given targetInput
   *
   * @param <T> The data type of the input being resolved.
   * @param <DV> The dependency whose input is being resolved.
   */
  public static final class ResolveFanoutStage<T, DV extends Vajram<?>> {

    private final VajramInputTypeSpec<T, DV> targetInput;

    ResolveFanoutStage(VajramInputTypeSpec<T, DV> targetInput) {
      this.targetInput = targetInput;
    }

    public <S, CV extends Vajram<?>> FanoutResolverStage<S, T, CV, DV> using(
        VajramInputTypeSpec<S, CV> sourceInput) {
      return new FanoutResolverStage<>(targetInput, sourceInput);
    }
  }
}
