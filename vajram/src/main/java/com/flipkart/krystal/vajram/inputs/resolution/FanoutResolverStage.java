package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.fanoutResolutionHelper;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.SkipPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
  private final VajramDependencyTypeSpec<?, CV, DV> dependency;
  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

  FanoutResolverStage(
      VajramDependencyTypeSpec<?, CV, DV> dependency,
      VajramInputTypeSpec<T, DV> targetInput,
      VajramInputTypeSpec<S, CV> sourceInput) {
    this.dependency = dependency;
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

  public InputResolver asResolver(Function<S, Collection<T>> transformer) {
    return new AbstractSimpleInputResolver(dependency, targetInput, ImmutableList.of(sourceInput)) {
      @Override
      public DependencyCommand<Inputs> resolve(
          String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
        return fanoutResolutionHelper(
            sourceInput, targetInput, transformer, skipConditions, inputs);
      }
    };
  }

  public static final class ResolveFanoutStage<T, CV extends Vajram<?>, DV extends Vajram<?>> {

    private final VajramDependencyTypeSpec<?, CV, DV> dependency;
    private final VajramInputTypeSpec<T, DV> targetInput;

    ResolveFanoutStage(
        VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> targetInput) {
      this.dependency = dependency;
      this.targetInput = targetInput;
    }

    public <S> FanoutResolverStage<S, T, CV, DV> using(VajramInputTypeSpec<S, CV> sourceInput) {
      return new FanoutResolverStage<>(dependency, targetInput, sourceInput);
    }
  }
}
