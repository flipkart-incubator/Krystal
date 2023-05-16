package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.resolutionHelper;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.SkipPredicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public final class SingleTargetResolverUsingStage<T, CV extends Vajram<?>, DV extends Vajram<?>> {
  private final VajramDependencyTypeSpec<?, CV, DV> dependency;
  private final VajramInputTypeSpec<T, DV> targetInput;

  SingleTargetResolverUsingStage(
      VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> targetInput) {
    this.dependency = dependency;
    this.targetInput = targetInput;
  }

  public SingleTargetAsIsResolverBuilder<T, CV, DV> usingAsIs(
      VajramInputTypeSpec<T, CV> sourceInput) {
    return new SingleTargetAsIsResolverBuilder<>(dependency, targetInput, sourceInput);
  }

  public <S> SingleTargetResolverBuilder<S, T, CV, DV> using(
      VajramInputTypeSpec<S, CV> sourceInput) {
    return new SingleTargetResolverBuilder<>(dependency, targetInput, sourceInput);
  }

  public static sealed class SingleTargetResolverBuilder<
      S, T, CV extends Vajram<?>, DV extends Vajram<?>> {
    private final VajramDependencyTypeSpec<?, CV, DV> dependency;
    private final VajramInputTypeSpec<T, DV> targetInput;
    private final VajramInputTypeSpec<S, CV> sourceInput;
    private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

    private SingleTargetResolverBuilder(
        VajramDependencyTypeSpec<?, CV, DV> dependency,
        VajramInputTypeSpec<T, DV> targetInput,
        VajramInputTypeSpec<S, CV> sourceInput) {
      this.dependency = dependency;
      this.targetInput = targetInput;
      this.sourceInput = sourceInput;
    }

    public SingleTargetResolverBuilder<S, T, CV, DV> skipIf(
        Predicate<Optional<S>> whenToSkip, String reason) {
      this.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
      return this;
    }

    public InputResolver asResolver(Function<S, T> transformer) {
      return new AbstractSimpleInputResolver(
          dependency, targetInput, ImmutableList.of(sourceInput)) {
        @Override
        public DependencyCommand<Inputs> resolve(
            String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
          return resolutionHelper(sourceInput, targetInput, transformer, skipConditions, inputs);
        }
      };
    }
  }

  public static final class SingleTargetAsIsResolverBuilder<
          T, CV extends Vajram<?>, DV extends Vajram<?>>
      extends SingleTargetResolverBuilder<T, T, CV, DV> {
    private SingleTargetAsIsResolverBuilder(
        VajramDependencyTypeSpec<?, CV, DV> dependency,
        VajramInputTypeSpec<T, DV> targetInput,
        VajramInputTypeSpec<T, CV> sourceInput) {
      super(dependency, targetInput, sourceInput);
    }

    public InputResolver asResolver() {
      return asResolver(Function.identity());
    }
  }
}
