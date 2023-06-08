package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.InputResolverUtil.SkipPredicate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
public final class InputResolverSpec<S, T, CV extends Vajram<?>, DV extends Vajram<?>> {

  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions;
  private final Function<Optional<S>, T> transformer;
  private final Function<Optional<S>, ? extends Collection<? extends T>> fanoutTransformer;

  InputResolverSpec(
      VajramInputTypeSpec<T, DV> targetInput,
      VajramInputTypeSpec<S, CV> sourceInput,
      List<SkipPredicate<S>> skipConditions,
      Function<Optional<S>, T> transformer,
      Function<Optional<S>, ? extends Collection<? extends T>> fanoutTransformer) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
    this.skipConditions = skipConditions;
    this.transformer = transformer;
    this.fanoutTransformer = fanoutTransformer;
  }
}
