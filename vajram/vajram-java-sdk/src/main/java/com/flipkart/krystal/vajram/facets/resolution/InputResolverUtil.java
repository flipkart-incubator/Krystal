package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.data.Errable.nil;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.data.Success;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.QualifiedInputs;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.VajramDepFanoutTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramDepSingleTypeSpec;
import com.flipkart.krystal.vajram.facets.VajramDependencySpec;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  public static ResolutionResult multiResolve(
      List<ResolutionRequest> resolutionRequests,
      Map<Integer, Collection<? extends SimpleInputResolver<?, ?, ?, ?>>> resolvers,
      Facets facets) {

    Map<Integer, List<RequestBuilder<Object>>> results = new LinkedHashMap<>();
    Map<Integer, DependencyCommand<? extends Request<Object>>> skippedDependencies =
        new LinkedHashMap<>();
    for (ResolutionRequest resolutionRequest : resolutionRequests) {
      int dependencyId = resolutionRequest.dependencyId();
      Collection<? extends SimpleInputResolver<?, ?, ?, ?>> depResolvers =
          resolvers.getOrDefault(dependencyId, List.of());
      for (SimpleInputResolver<?, ?, ?, ?> simpleResolver : depResolvers) {
        int resolvable = simpleResolver.getResolverSpec().targetInput().id();
        //noinspection unchecked,rawtypes
        DependencyCommand<?> command =
            _resolutionHelper(
                (List) simpleResolver.getResolverSpec().sourceInputs(),
                simpleResolver.getResolverSpec().transformer(),
                simpleResolver.getResolverSpec().fanoutTransformer(),
                simpleResolver.getResolverSpec().skipConditions(),
                facets);
        if (command.shouldSkip()) {
          skippedDependencies.put(dependencyId, skipExecution(command.doc()));
          break;
        }
        collectDepInputs(resolutionRequest.depRequests(), resolvable, command);
      }
      if (!skippedDependencies.containsKey(dependencyId)) {
        results.putIfAbsent(dependencyId, resolutionRequest.depRequests());
      }
    }
    return new ResolutionResult(results, skippedDependencies);
  }

  public static void collectDepInputs(
      List<RequestBuilder<Object>> depReqs,
      @Nullable Integer resolvable,
      DependencyCommand<?> command) {
    if (command.shouldSkip()) {
      return;
    }
    if (command instanceof SingleExecute<?> singleExecute) {
      depReqs.forEach(request -> handleResolverReturn(resolvable, singleExecute.input(), request));
    } else if (command instanceof MultiExecute<?> multiExecute) {
      Collection<?> objects = multiExecute.multiInputs();
      if (depReqs.size() > 1) {
        throw new IllegalArgumentException("A varjam can have at most one fanout resolver.");
      } else if (depReqs.isEmpty()) {
        throw new IllegalArgumentException("This should not be possible");
      }
      RequestBuilder<Object> depReq = depReqs.get(0);
      boolean first = true;

      for (Object object : objects) {
        if (first) {
          first = false;
          handleResolverReturn(resolvable, object, depReq);
        } else {
          RequestBuilder<Object> newReq = depReq._newCopy();
          handleResolverReturn(resolvable, object, newReq);
          depReqs.add(newReq);
        }
      }
    }
  }

  private static void handleResolverReturn(
      @Nullable Integer resolvable, @Nullable Object o, RequestBuilder<?> requestBuilder) {
    if (o instanceof Request<?> resolvedRequest) {
      resolvedRequest._asMap().forEach(requestBuilder::_set);
    } else if (resolvable != null) {
      requestBuilder._set(resolvable, Errable.withValue(o));
    } else {
      throw new AssertionError(
          "Resolvable is null and resolver return is not of Facets. This should not be possible");
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
        inputValue =
            facets._getDepResponses(sourceInput.id()).requestResponses().stream()
                .map(RequestResponse::response)
                .iterator()
                .next();
      } else if (sourceInput instanceof VajramDepFanoutTypeSpec<?, ?, ?>) {
        inputValue =
            Errable.withValue(
                facets._getDepResponses(sourceInput.id()).requestResponses().stream()
                    .map(RequestResponse::response)
                    .filter(e -> e instanceof Success<?>)
                    .map(e -> (Success<?>) e)
                    .map(Success::value)
                    .toList());
      } else if (sourceInput != null) {
        inputValue = facets._getErrable(sourceInput.id());
      } else {
        inputValue = nil();
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
        return MultiExecute.skipFanout(skipPredicate.get().reason());
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

  public static <T, CV extends Request<?>, DV extends Request<?>> InputResolver toResolver(
      VajramDependencySpec<?, ?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> spec) {
    if (spec.canFanout()) {
      return new SimpleFanoutInputResolver<>(dependency, spec);
    } else {
      return new SimpleSingleInputResolver<>(dependency, spec);
    }
  }

  private InputResolverUtil() {}

  public static Comparator<QualifiedInputs> getQualifiedInputsComparator() {
    return comparingInt(QualifiedInputs::dependencyId)
        .thenComparingInt(q -> q.inputNames().size())
        .thenComparing(
            QualifiedInputs::inputNames,
            // Two resolvers resoving the same dependency cannot have a common inputName
            // So we can just compare the lexicographically first elements of both
            // inputNames to get a deterministic comparator
            comparing(strings -> strings.stream().sorted().findFirst().orElse("")));
  }
}