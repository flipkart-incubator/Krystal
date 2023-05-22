package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.resolutionHelper;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.SkipPredicate;
import com.flipkart.krystal.vajram.inputs.resolution.ResolverStage.AsIsResolverStage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
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
public sealed class ResolverStage<S, T, CV extends Vajram<?>, DV extends Vajram<?>>
    permits AsIsResolverStage {
  private final VajramDependencyTypeSpec<?, CV, DV> dependency;
  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

  ResolverStage(
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
  public ResolverStage<S, T, CV, DV> skipIf(Predicate<Optional<S>> whenToSkip, String reason) {
    this.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
    return this;
  }

  public InputResolver asResolver(Function<S, T> transformer) {
    return new AbstractSimpleInputResolver(dependency, targetInput, ImmutableList.of(sourceInput)) {
      @Override
      public DependencyCommand<Inputs> resolve(
          String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
        return resolutionHelper(sourceInput, targetInput, transformer, skipConditions, inputs);
      }
    };
  }

  public static final class AsIsResolverStage<T, CV extends Vajram<?>, DV extends Vajram<?>>
      extends ResolverStage<T, T, CV, DV> {
    AsIsResolverStage(
        VajramDependencyTypeSpec<?, CV, DV> dependency,
        VajramInputTypeSpec<T, DV> targetInput,
        VajramInputTypeSpec<T, CV> sourceInput) {
      super(dependency, targetInput, sourceInput);
    }

    @Override
    public AsIsResolverStage<T, CV, DV> skipIf(Predicate<Optional<T>> whenToSkip, String reason) {
      super.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
      return this;
    }

    public InputResolver asResolver() {
      return asResolver(Function.identity());
    }
  }

  public static final class ResolveStage<T, CV extends Vajram<?>, DV extends Vajram<?>> {
    private final VajramDependencyTypeSpec<?, CV, DV> dependency;
    private final VajramInputTypeSpec<T, DV> targetInput;

    ResolveStage(
        VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> targetInput) {
      this.dependency = dependency;
      this.targetInput = targetInput;
    }

    /**
     * Use the value of the source input as-is, without any transformation. If the source input is
     * {@link ValueOrError#empty()}, then the resolved value will also be {@link
     * ValueOrError#empty()}. This is possible only if the dataType of the sourceInput and the
     * target input are same.
     *
     * @see #using(VajramInputTypeSpec)
     * @param sourceInput the spec of the source input being used for resolution
     */
    public AsIsResolverStage<T, CV, DV> usingAsIs(VajramInputTypeSpec<T, CV> sourceInput) {
      return new AsIsResolverStage<>(dependency, targetInput, sourceInput);
    }

    /**
     * Use the value of the source input and transform it to compute the resolved value.
     *
     * @param sourceInput the spec of the source input being used for resolution
     */
    public <S> ResolverStage<S, T, CV, DV> using(VajramInputTypeSpec<S, CV> sourceInput) {
      return new ResolverStage<>(dependency, targetInput, sourceInput);
    }
  }
}
