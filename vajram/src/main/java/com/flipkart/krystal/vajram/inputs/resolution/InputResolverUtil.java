package com.flipkart.krystal.vajram.inputs.resolution;

import static com.flipkart.krystal.data.ValueOrError.empty;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.executeFanoutWith;
import static com.flipkart.krystal.vajram.inputs.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.executeWith;
import static com.flipkart.krystal.vajram.inputs.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public final class InputResolverUtil {

  public static ResolutionResult multiResolve(
      List<ResolutionRequest> resolutionRequests,
      Map<String, Collection<SimpleInputResolver>> resolvers,
      Inputs inputs) {

    Map<String, List<Map<String, Object>>> results = new LinkedHashMap<>();
    Map<String, DependencyCommand<Inputs>> skippedDependencies = new LinkedHashMap<>();
    for (ResolutionRequest resolutionRequest : resolutionRequests) {
      String dependencyName = resolutionRequest.dependencyName();
      List<Map<String, Object>> depInputs = new ArrayList<>();
      Collection<SimpleInputResolver> depResolvers = resolvers.get(dependencyName);
      for (SimpleInputResolver simpleResolver : depResolvers) {
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
      List<Map<String, Object>> depInputs, String resolvable, DependencyCommand<?> command) {
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
              LinkedHashMap<String, Object> e = new LinkedHashMap<>();
              depInputs.add(e);
              handleResolverReturn(resolvable, o, e);
            });
      } else {
        List<Map<String, Object>> more =
            new ArrayList<>(depInputs.size() * objects.size() - depInputs.size());
        for (Map<String, Object> depInput : depInputs) {
          boolean first = true;
          for (Object object : objects) {
            if (first) {
              first = false;
              handleResolverReturn(resolvable, object, depInput);
            } else {
              LinkedHashMap<String, Object> e = new LinkedHashMap<>(depInput);
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
      String resolvable, Object o, Map<String, Object> valuesMap) {
    if (o instanceof Inputs inputs) {
      //noinspection unchecked,rawtypes
      valuesMap.putAll(
          inputs.values().entrySet().stream()
              .collect(
                  toMap(Entry::getKey, e -> ((ValueOrError) e.getValue()).value().orElse(null))));
    } else {
      valuesMap.put(resolvable, o);
    }
  }

  static <S, T, CV extends Vajram<?>, DV extends Vajram<?>> InputResolver toResolver(
      VajramDependencyTypeSpec<?, CV, DV> dependency, SimpleInputResolverSpec<S, T, CV, DV> spec) {
    return new SimpleInputResolver(dependency, spec);
  }

  private static <T> DependencyCommand<T> _resolutionHelper(
      VajramInputTypeSpec<?, ?> sourceInput,
      Function<? extends Optional<?>, ?> oneToOneTransformer,
      Function<? extends Optional<?>, ? extends Collection<?>> fanoutTransformer,
      List<? extends SkipPredicate<?>> skipPredicates,
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

    //noinspection unchecked
    Optional<SkipPredicate<Object>> skipPredicate =
        skipPredicates.stream()
            .map(p -> (SkipPredicate<Object>) p)
            .filter(sSkipPredicate -> sSkipPredicate.condition().test(inputValue.value()))
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
      transformedInput =
          inputValue
              .value()
              .map(
                  t -> {
                    return transformer.apply(Optional.of(t));
                  });
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

  public record ResolutionResult(
      Map<String, List<Map<String, Object>>> results,
      Map<String, DependencyCommand<Inputs>> skippedDependencies) {}

  private InputResolverUtil() {}
}
