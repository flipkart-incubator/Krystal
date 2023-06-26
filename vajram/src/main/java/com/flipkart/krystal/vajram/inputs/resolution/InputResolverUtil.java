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
import static java.util.stream.Collectors.toMap;

import com.flipkart.krystal.data.InputValue;
import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.DependencyCommand;
import com.flipkart.krystal.vajram.inputs.MultiExecute;
import com.flipkart.krystal.vajram.inputs.SingleExecute;
import com.flipkart.krystal.vajram.inputs.VajramDependencyTypeSpec;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.internal.AbstractSimpleInputResolver;
import com.flipkart.krystal.vajram.inputs.resolution.internal.SkipPredicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;

public final class InputResolverUtil {

  public static ImmutableMap<String, DependencyCommand<Inputs>> multiResolve(
      List<ResolutionRequest> resolutionRequests,
      Collection<InputResolverDefinition> resolvers,
      Inputs inputs) {
    Map<String, AbstractSimpleInputResolver> resolversByTarget =
        resolvers.stream()
            .collect(
                toMap(
                    resolver ->
                        ((AbstractSimpleInputResolver) resolver)
                            .getResolverSpec()
                            .getTargetInput()
                            .name(),
                    inputResolver -> (AbstractSimpleInputResolver) inputResolver));
    Map<String, DependencyCommand<Inputs>> results = new LinkedHashMap<>();
    for (ResolutionRequest resolutionRequest : resolutionRequests) {
      String dependencyName = resolutionRequest.dependencyName();
      ImmutableSet<String> inputsToResolve = resolutionRequest.inputsToResolve();
      List<Map<String, Object>> depInputs = new ArrayList<>();
      for (String resolvable : inputsToResolve) {
        AbstractSimpleInputResolver resolver = resolversByTarget.get(resolvable);
        DependencyCommand<?> command =
            _resolutionHelper(
                resolver.getResolverSpec().getSourceInput(),
                resolver.getResolverSpec().getTransformer(),
                resolver.getResolverSpec().getFanoutTransformer(),
                resolver.getResolverSpec().getSkipConditions(),
                inputs);
        if (command.shouldSkip()) {
          //noinspection unchecked
          results.put(dependencyName, (DependencyCommand<Inputs>) command);
          break;
        } else if (command instanceof SingleExecute<?> singleExecute) {
          if (depInputs.isEmpty()) {
            LinkedHashMap<String, Object> e = new LinkedHashMap<>();
            e.put(resolvable, singleExecute.input());
            depInputs.add(e);
          } else {
            depInputs.forEach(map -> map.put(resolvable, singleExecute.input()));
          }
        } else if (command instanceof MultiExecute<?> multiExecute) {
          Collection<?> objects = multiExecute.multiInputs();
          if (depInputs.isEmpty()) {
            objects.forEach(
                o -> {
                  LinkedHashMap<String, Object> e = new LinkedHashMap<>();
                  e.put(resolvable, o);
                  depInputs.add(e);
                });
          } else {
            List<Map<String, Object>> more =
                new ArrayList<>(depInputs.size() * objects.size() - depInputs.size());
            for (Map<String, Object> depInput : depInputs) {
              boolean first = true;
              for (Object object : objects) {
                if (first) {
                  first = false;
                  depInput.put(resolvable, object);
                } else {
                  LinkedHashMap<String, Object> e = new LinkedHashMap<>();
                  e.put(resolvable, object);
                  more.add(e);
                }
              }
            }
            depInputs.addAll(more);
          }
        }
      }
      if (depInputs.size() <= 1) {
        Map<String, InputValue<Object>> collect =
            depInputs.get(0).entrySet().stream()
                .collect(toMap(Entry::getKey, e -> withValue(e.getValue())));
        DependencyCommand<Inputs> tSingleExecute = executeWith(new Inputs(collect));
        results.put(dependencyName, tSingleExecute);
      } else {
        List<Inputs> inputsList = new ArrayList<>();
        for (Map<String, Object> depInput : depInputs) {
          inputsList.add(
              new Inputs(
                  depInput.entrySet().stream()
                      .collect(toMap(Entry::getKey, e -> withValue(e.getValue())))));
        }
        results.put(dependencyName, executeFanoutWith(inputsList));
      }
    }
    return ImmutableMap.copyOf(results);
  }

  static <S, T, CV extends Vajram<?>, DV extends Vajram<?>> InputResolver toResolver(
      VajramDependencyTypeSpec<?, CV, DV> dependency, InputResolverSpec<S, T, CV, DV> spec) {
    return new AbstractSimpleInputResolver(dependency, spec) {
      @Override
      public DependencyCommand<Inputs> resolve(
          String dependencyName, ImmutableSet<String> inputsToResolve, Inputs inputs) {
        throw new UnsupportedOperationException(
            "This should not be called. See InputResolverUtil.multiResolve");
      }
    };
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

  private InputResolverUtil() {}
}
