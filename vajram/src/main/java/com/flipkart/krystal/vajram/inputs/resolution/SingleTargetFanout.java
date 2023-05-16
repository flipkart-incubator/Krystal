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

public final class SingleTargetFanout<S, T, CV extends Vajram<?>, DV extends Vajram<?>> {
  private final VajramDependencyTypeSpec<?, CV, DV> dependency;
  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

  SingleTargetFanout(
      VajramDependencyTypeSpec<?, CV, DV> dependency,
      VajramInputTypeSpec<T, DV> targetInput,
      VajramInputTypeSpec<S, CV> sourceInput) {
    this.dependency = dependency;
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  public SingleTargetFanout<S, T, CV, DV> skipIf(String reason, Predicate<Optional<S>> whenToSkip) {
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

  public static final class SingleTargetFanoutIRStage<
      T, CV extends Vajram<?>, DV extends Vajram<?>> {

    private final VajramDependencyTypeSpec<?, CV, DV> dependency;
    private final VajramInputTypeSpec<T, DV> targetInput;

    SingleTargetFanoutIRStage(
        VajramDependencyTypeSpec<?, CV, DV> dependency, VajramInputTypeSpec<T, DV> targetInput) {
      this.dependency = dependency;
      this.targetInput = targetInput;
    }

    public <S> SingleTargetFanout<S, T, CV, DV> using(VajramInputTypeSpec<S, CV> sourceInput) {
      return new SingleTargetFanout<>(dependency, targetInput, sourceInput);
    }
  }
}
