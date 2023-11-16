package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.VajramDepFanoutTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramDepSingleTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramDependencySpec;
import com.flipkart.krystal.vajram.inputs.VajramFacetSpec;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  public static ResolutionResult multiResolve(
      List<ResolutionRequest> resolutionRequests,
      Map<String, Collection<? extends SimpleInputResolver<?, ?, ?>>> resolvers,
      Inputs inputs) {

    Map<String, List<Map<String, @Nullable Object>>> results = new LinkedHashMap<>();
    Map<String, DependencyCommand<Inputs>> skippedDependencies = new LinkedHashMap<>();
    for (ResolutionRequest resolutionRequest : resolutionRequests) {
      String dependencyName = resolutionRequest.dependencyName();
      List<Map<String, @Nullable Object>> depInputs = new ArrayList<>();
      Collection<? extends SimpleInputResolver<?, ?, ?>> depResolvers =
          resolvers.getOrDefault(dependencyName, List.of());
      for (SimpleInputResolver<?, ?, ?> simpleResolver : depResolvers) {
        String resolvable = simpleResolver.getResolverSpec().getTargetInput().name();
        DependencyCommand<?> command =
            _resolutionHelper(
                simpleResolver.getResolverSpec().getSourceInput(),
                simpleResolver.getResolverSpec().getTransformer(),
                simpleResolver.getResolverSpec().getFanoutTransformer(),
                simpleResolver.getResolverSpec().getSkipConditions(),
                inputs);
        if (command.shouldSkip()) {
          //noinspection unchecked
          skippedDependencies.put(dependencyName, (DependencyCommand<Inputs>) command);
          break;
        }
        collectDepInputs(depInputs, resolvable, command);
      }
      if (!skippedDependencies.containsKey(dependencyName)) {
        results.putIfAbsent(dependencyName, depInputs);
      }
    }
    return new ResolutionResult(results, skippedDependencies);
  }

  public static void collectDepInputs(
      List<Map<String, @Nullable Object>> depInputs,
      @Nullable String resolvable,
      DependencyCommand<?> command) {
    if (command.shouldSkip()) {
      return;
    }
    if (command instanceof SingleExecute<?> singleExecute) {
      if (depInputs.isEmpty()) {
        depInputs.add(new LinkedHashMap<>());
      }
      depInputs.forEach(map -> handleResolverReturn(resolvable, singleExecute.input(), map));
    } else if (command instanceof MultiExecute<?> multiExecute) {
      Collection<?> objects = multiExecute.multiInputs();
      if (depInputs.isEmpty()) {
        objects.forEach(
            o -> {
              LinkedHashMap<String, @Nullable Object> e = new LinkedHashMap<>();
              depInputs.add(e);
              handleResolverReturn(resolvable, o, e);
            });
      } else {
        List<Map<String, @Nullable Object>> more =
            new ArrayList<>(depInputs.size() * objects.size() - depInputs.size());
        for (Map<String, @Nullable Object> depInput : depInputs) {
          boolean first = true;
          ImmutableMap<String, @Nullable Object> originalDepInput = ImmutableMap.copyOf(depInput);
          for (Object object : objects) {
            if (first) {
              first = false;
              handleResolverReturn(resolvable, object, depInput);
            } else {
              LinkedHashMap<String, @Nullable Object> e = new LinkedHashMap<>(originalDepInput);
              more.add(e);
              handleResolverReturn(resolvable, object, e);
            }
          }
        }
        depInputs.addAll(more);
      }
    }
  }

  private static void handleResolverReturn(
      @Nullable String resolvable, @Nullable Object o, Map<String, @Nullable Object> valuesMap) {
    if (o instanceof Inputs inputs) {
      for (Entry<String, InputValue<Object>> e : inputs.values().entrySet()) {
        //noinspection unchecked,rawtypes
        if (valuesMap.put(e.getKey(), ((ValueOrError) e.getValue()).value().orElse(null)) != null) {
          throw new IllegalStateException("Duplicate key");
        }
      }
    } else if (resolvable != null) {
      valuesMap.put(resolvable, o);
    } else {
      throw new AssertionError(
          "Resolvable is null and resolver return is not of Inputs. This should not be possible");
    }
  }

  static <T> DependencyCommand<T> _resolutionHelper(
      @Nullable VajramFacetSpec<?> sourceInput,
      @Nullable Function<? extends Optional<?>, ?> oneToOneTransformer,
      @Nullable Function<? extends Optional<?>, ? extends Collection<?>> fanoutTransformer,
      List<? extends SkipPredicate<?>> skipPredicates,
      Inputs inputs) {
    boolean fanout = fanoutTransformer != null;
    final Optional<Object> inputValue;
    if (sourceInput instanceof VajramDepSingleTypeSpec<?, ?>) {
      inputValue =
          inputs.getDepValue(sourceInput.name()).values().values().iterator().next().value();
    } else if (sourceInput instanceof VajramDepFanoutTypeSpec<?, ?>) {
      inputValue =
          Optional.of(
              inputs.getDepValue(sourceInput.name()).values().values().stream()
                  .map(ValueOrError::value)
                  .filter(Optional::isPresent)
                  .map(Optional::get)
                  .toList());
    } else if (sourceInput != null) {
      inputValue = inputs.getInputValue(sourceInput.name()).value();
    } else {
      inputValue = Optional.empty();
    }

    //noinspection unchecked
    Optional<SkipPredicate<Object>> skipPredicate =
        skipPredicates.stream()
            .map(p -> (SkipPredicate<Object>) p)
            .filter(sSkipPredicate -> sSkipPredicate.condition().test(inputValue))
            .findFirst();
    if (skipPredicate.isPresent()) {
      if (fanout) {
        return skipFanout(skipPredicate.get().reason());
      } else {
        return skipExecution(skipPredicate.get().reason());
      }
    }
    //noinspection unchecked
    Function<Optional<Object>, Object> transformer =
        ofNullable((Function<Optional<Object>, Object>) oneToOneTransformer)
            .or(
                () ->
                    ofNullable(fanoutTransformer)
                        .map(
                            function ->
                                (Function<Optional<Object>, Collection<Object>>)
                                    function.andThen(objects -> (Collection<Object>) objects))
                        .map(x -> x.andThen(identity())))
            .orElse(Optional::orElseThrow);
    Optional<Object> transformedInput;
    if (sourceInput == null) {
      transformedInput = ofNullable(transformer.apply(Optional.empty()));
    } else {
      transformedInput = inputValue.map(t -> transformer.apply(Optional.of(t)));
    }
    if (fanout) {
      //noinspection unchecked
      return executeFanoutWith(
          transformedInput.map(ts -> ((Collection<T>) ts).stream()).stream()
              .flatMap(identity())
              .collect(toImmutableList()));
    } else {
      //noinspection unchecked
      return executeWith((T) transformedInput.orElse(null));
    }
  }

  static <S, T, CV extends Vajram<?>> InputResolver toResolver(
      VajramDependencySpec<?, CV> dependency, SimpleInputResolverSpec<S, T> spec) {
    return new SimpleInputResolver<>(dependency, spec);
  }

  public record ResolutionResult(
      Map<String, List<Map<String, @Nullable Object>>> results,
      Map<String, DependencyCommand<Inputs>> skippedDependencies) {}

  private InputResolverUtil() {}
}
