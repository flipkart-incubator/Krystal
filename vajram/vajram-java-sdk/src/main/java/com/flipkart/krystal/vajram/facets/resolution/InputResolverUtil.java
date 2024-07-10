package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.utils.SkippedExecutionException;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.VajramDepFanoutTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramDepSingleTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  public static ResolutionResult multiResolve(
      List<ResolutionRequest> resolutionRequests,
      Map<String, Collection<? extends SimpleInputResolver<?, ?, ?, ?>>> resolvers,
      Facets facets) {

    Map<String, List<Map<String, @Nullable Object>>> results = new LinkedHashMap<>();
    Map<String, DependencyCommand<Facets>> skippedDependencies = new LinkedHashMap<>();
    for (ResolutionRequest resolutionRequest : resolutionRequests) {
      String dependencyName = resolutionRequest.dependencyName();
      List<Map<String, @Nullable Object>> depInputs = new ArrayList<>();
      Collection<? extends SimpleInputResolver<?, ?, ?, ?>> depResolvers =
          resolvers.getOrDefault(dependencyName, List.of());
      for (SimpleInputResolver<?, ?, ?, ?> simpleResolver : depResolvers) {
        String resolvable = simpleResolver.getResolverSpec().targetInput().name();
        DependencyCommand<?> command;
        var fanoutTransformer = simpleResolver.getResolverSpec().fanoutTransformer();
        boolean fanout = fanoutTransformer != null;
        try {
          //noinspection unchecked,rawtypes
          command =
              _resolutionHelper(
                  (List) simpleResolver.getResolverSpec().sourceInputs(),
                  simpleResolver.getResolverSpec().transformer(),
                  fanoutTransformer,
                  simpleResolver.getResolverSpec().skipConditions(),
                  facets);
        } catch (Throwable e) {
          command = handleResolverException(e, fanout);
        }
        if (command.shouldSkip()) {
          //noinspection unchecked
          skippedDependencies.put(dependencyName, (DependencyCommand<Facets>) command);
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

  public static <T> @NonNull DependencyCommand<T> handleResolverException(
      Throwable e, boolean fanout) {
    return handleResolverException(e, fanout, "Resolver threw exception.");
  }

  public static <T> @NonNull DependencyCommand<T> handleResolverException(
      Throwable e, boolean fanout, String messagePrefix) {
    DependencyCommand<T> command;
    String exceptionMessage = e.getMessage();
    if (e instanceof SkippedExecutionException skippedExecutionException) {
      exceptionMessage = skippedExecutionException.getMessage();
    }
    if (exceptionMessage == null) {
      exceptionMessage = messagePrefix + ' ' + e;
    }
    if (fanout) {
      command = skipFanout(exceptionMessage);
    } else {
      command = skipExecution(exceptionMessage);
    }
    return command;
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
    if (o instanceof Facets facets) {
      for (Entry<String, FacetValue<Object>> e : facets.values().entrySet()) {
        //noinspection unchecked,rawtypes
        if (valuesMap.put(e.getKey(), ((Errable) e.getValue()).value().orElse(null)) != null) {
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

  @SuppressWarnings("rawtypes")
  static <T> DependencyCommand<T> _resolutionHelper(
      List<VajramFacetSpec> sourceInputs,
      @Nullable Function<List<Errable<?>>, ?> oneToOneTransformer,
      @Nullable Function<List<Errable<?>>, ? extends Collection<?>> fanoutTransformer,
      List<? extends SkipPredicate<?>> skipPredicates,
      Facets facets) {
    boolean fanout = fanoutTransformer != null;
    List<Errable<?>> inputValues = new ArrayList<>();
    for (VajramFacetSpec sourceInput : sourceInputs) {
      final Errable<Object> inputValue;
      if (sourceInput instanceof VajramDepSingleTypeSpec<?, ?, ?>) {
        inputValue = facets.getDepValue(sourceInput.name()).values().values().iterator().next();
      } else if (sourceInput instanceof VajramDepFanoutTypeSpec<?, ?, ?>) {
        inputValue =
            Errable.withValue(
                facets.getDepValue(sourceInput.name()).values().values().stream()
                    .map(Errable::value)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList());
      } else if (sourceInput != null) {
        inputValue = facets.getInputValue(sourceInput.name());
      } else {
        inputValue = Errable.empty();
      }
      inputValues.add(inputValue);
    }

    //noinspection unchecked
    Optional<SkipPredicate<Object>> skipPredicate =
        skipPredicates.stream()
            .map(p -> (SkipPredicate<Object>) p)
            .filter(sSkipPredicate -> sSkipPredicate.condition().test(inputValues))
            .findFirst();
    if (skipPredicate.isPresent()) {
      if (fanout) {
        return skipFanout(skipPredicate.get().reason());
      } else {
        return skipExecution(skipPredicate.get().reason());
      }
    }
    //noinspection unchecked
    Function<List<Errable<?>>, Object> transformer =
        ofNullable((Function<List<Errable<?>>, Object>) oneToOneTransformer)
            .or(
                () ->
                    ofNullable(fanoutTransformer)
                        .map(
                            function ->
                                (Function<List<Errable<?>>, Collection<Object>>)
                                    function.andThen(objects -> (Collection<Object>) objects))
                        .map(x -> x.andThen(identity())))
            .orElse(
                errables -> {
                  throw new AssertionError();
                });
    Optional<Object> transformedInput = ofNullable(transformer.apply(inputValues));
    if (fanout) {
      //noinspection unchecked
      return MultiExecute.executeFanoutWith(
          transformedInput.map(ts -> ((Collection<T>) ts).stream()).stream()
              .flatMap(identity())
              .collect(toImmutableList()));
    } else {
      //noinspection unchecked
      return SingleExecute.executeWith((T) transformedInput.orElse(null));
    }
  }

  public static <T, CV extends VajramRequest<?>, DV extends VajramRequest<?>>
      InputResolver toResolver(
          VajramDependencySpec<?, ?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> spec) {
    return new SimpleInputResolver<>(dependency, spec);
  }

  public record ResolutionResult(
      Map<String, List<Map<String, @Nullable Object>>> results,
      Map<String, DependencyCommand<Facets>> skippedDependencies) {}

  private InputResolverUtil() {}
}
