package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.flipkart.krystal.data.ValueOrError.withValue;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

final class InputResolverUtil {

  private static <S, T, CV extends Vajram<?>, DV extends Vajram<?>>
      DependencyCommand<Inputs> _resolutionHelper(
          @Nullable VajramInputTypeSpec<S, CV> sourceInput,
          VajramInputTypeSpec<T, DV> targetInput,
          Function<Optional<S>, T> oneToOneTransformer,
          Function<Optional<S>, ? extends Collection<? extends T>> fanoutTransformer,
          List<SkipPredicate<S>> skipPredicates,
          Inputs inputs) {
    boolean fanout = fanoutTransformer != null;
    ValueOrError<Object> inputValue;
    if (sourceInput instanceof VajramDependencyTypeSpec<?, ?, ?>) {
      inputValue = inputs.getDepValue(sourceInput.name()).values().values().iterator().next();
    } else if (sourceInput != null) {
      inputValue = inputs.getInputValue(sourceInput.name());
    } else {
      inputValue = empty();
    }

    Optional<SkipPredicate<S>> skipPredicate =
        skipPredicates.stream()
            .filter(
                sSkipPredicate -> {
                  //noinspection unchecked
                  Optional<S> value = (Optional<S>) inputValue.value();
                  return sSkipPredicate.condition().test(value);
                })
            .findFirst();
    if (skipPredicate.isPresent()) {
      if (fanout) {
        return skipFanout(skipPredicate.get().reason());
      } else {
        return skipExecution(skipPredicate.get().reason());
      }
    }
    //noinspection unchecked
    Function<Optional<S>, Object> transformer =
        ofNullable((Function<Optional<S>, Object>) oneToOneTransformer)
            .or(() -> ofNullable(fanoutTransformer).map(x -> x.andThen(identity())))
            .orElse(Optional::orElseThrow);
    Optional<Object> transformedInput;
    if (sourceInput == null) {
      transformedInput = ofNullable(transformer.apply(Optional.empty()));
    } else {
      transformedInput =
          inputValue
              .value()
              .map(
                  t -> {
                    //noinspection unchecked
                    return transformer.apply((Optional<S>) ofNullable(t));
                  });
    }
    Function<T, Inputs> valueToInput =
        t -> new Inputs(ImmutableMap.of(targetInput.name(), withValue(t)));
    if (fanout) {
      //noinspection unchecked
      return executeFanoutWith(
          transformedInput.map(ts -> ((Collection<T>) ts).stream().map(valueToInput)).stream()
              .flatMap(identity())
              .collect(toImmutableList()));
    } else {
      //noinspection unchecked
      return executeWith(transformedInput.map(o -> (T) o).map(valueToInput).orElse(null));
    }
  }

  static <S, T, CV extends Vajram<?>, DV extends Vajram<?>> InputResolver toResolver(
      VajramDependencyTypeSpec<?, CV, DV> dependency, InputResolverSpec<S, T, CV, DV> spec) {
    return new AbstractSimpleInputResolver(
        dependency,
        spec.getTargetInput(),
        ofNullable(spec.getSourceInput()).stream().collect(toImmutableList())) {
      @Override
      public DependencyCommand<Inputs> resolve(
          String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
        return _resolutionHelper(
            spec.getSourceInput(),
            spec.getTargetInput(),
            spec.getTransformer(),
            spec.getFanoutTransformer(),
            spec.getSkipConditions(),
            inputs);
      }
    };
  }

  record SkipPredicate<T>(String reason, Predicate<Optional<T>> condition) {}

  private InputResolverUtil() {}
}
