package com.flipkart.krystal.vajram.inputs.resolution;

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

final class InputResolverUtil {

  static <S, T, CV extends Vajram<?>, DV extends Vajram<?>>
      DependencyCommand<Inputs> resolutionHelper(
          VajramInputTypeSpec<S, CV> sourceInput,
          VajramInputTypeSpec<T, DV> targetInput,
          Function<S, T> oneToOneTransformer,
          List<SkipPredicate<S>> skipPredicates,
          Inputs inputs) {
    return _resolutionHelper(
        sourceInput, targetInput, oneToOneTransformer, null, skipPredicates, inputs);
  }

  static <S, T, CV extends Vajram<?>, DV extends Vajram<?>>
      DependencyCommand<Inputs> fanoutResolutionHelper(
          VajramInputTypeSpec<S, CV> sourceInput,
          VajramInputTypeSpec<T, DV> targetInput,
          Function<S, ? extends Collection<? extends T>> fanoutTransformer,
          List<SkipPredicate<S>> skipPredicates,
          Inputs inputs) {
    return _resolutionHelper(
        sourceInput, targetInput, null, fanoutTransformer, skipPredicates, inputs);
  }

  private static <S, T, CV extends Vajram<?>, DV extends Vajram<?>>
      DependencyCommand<Inputs> _resolutionHelper(
          VajramInputTypeSpec<S, CV> sourceInput,
          VajramInputTypeSpec<T, DV> targetInput,
          Function<S, T> oneToOneTransformer,
          Function<S, ? extends Collection<? extends T>> fanoutTransformer,
          List<SkipPredicate<S>> skipPredicates,
          Inputs inputs) {
    boolean fanout = fanoutTransformer != null;
    ValueOrError<Object> inputValue;
    if (sourceInput instanceof VajramDependencyTypeSpec<?, ?, ?>) {
      inputValue = inputs.getDepValue(sourceInput.name()).values().values().iterator().next();
    } else {
      inputValue = inputs.getInputValue(sourceInput.name());
    }
    Optional<SkipPredicate<S>> skipReason =
        skipPredicates.stream()
            .filter(
                sSkipPredicate -> {
                  //noinspection unchecked
                  Optional<S> value = (Optional<S>) inputValue.value();
                  return sSkipPredicate.condition().test(value);
                })
            .findFirst();
    if (skipReason.isPresent()) {
      if (fanout) {
        return skipFanout(skipReason.get().reason());
      } else {
        return skipExecution(skipReason.get().reason());
      }
    }
    //noinspection unchecked
    Function<S, Object> transformer =
        ofNullable((Function<S, Object>) oneToOneTransformer)
            .or(() -> ofNullable(fanoutTransformer).map(x -> x.andThen(ts -> ts)))
            .orElse((Function<S, Object>) identity());
    Optional<Object> transformedInput =
        inputValue
            .value()
            .map(
                t -> {
                  //noinspection unchecked
                  return transformer.apply((S) t);
                });
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
        dependency, spec.getTargetInput(), ImmutableList.of(spec.getSourceInput())) {
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
