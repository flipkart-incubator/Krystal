package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;

/**
 * The specification of a resolver which resolves exactly one input of a dependency.
 *
 * @param <S> The type of the input using which the resolution is happening.
 * @param <T> The type of the dependency's input which is being resolved.
 * @param <CV> The type of the vajram doing the resolution.
 * @param <DV> The type of the vajram whose input is being resolved.
 */
@Getter
public final class SimpleInputResolverSpec<S, T, CV extends Vajram<?>, DV extends Vajram<?>> {

  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions;
  private final Function<Optional<S>, T> transformer;
  private final Function<Optional<S>, ? extends Collection<? extends T>> fanoutTransformer;

  SimpleInputResolverSpec(
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
