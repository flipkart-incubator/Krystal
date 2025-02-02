package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.MultiExecute.skipFanout;
import static com.flipkart.krystal.vajram.facets.SingleExecute.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.MultiExecute;
import com.flipkart.krystal.vajram.facets.SingleExecute;
import com.flipkart.krystal.vajram.facets.specs.DefaultFacetSpec;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.FanoutDepSpec;
import com.flipkart.krystal.vajram.facets.specs.One2OneDepSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  public static ResolverCommand toResolverCommand(DependencyCommand<Builder> dependencyCommand) {
    if (dependencyCommand.shouldSkip()) {
      return ResolverCommand.skip(dependencyCommand.doc(), dependencyCommand.skipCause());
    }
    return ResolverCommand.executeWithRequests(dependencyCommand.inputs());
  }

  public static <T> DependencyCommand<T> handleResolverException(Throwable e, boolean fanout) {
    return handleResolverException(e, fanout, "Resolver threw exception.");
  }

  public static <T> DependencyCommand<T> handleResolverException(
      Throwable e, boolean fanout, String messagePrefix) {
    DependencyCommand<T> command;
    if (fanout) {
      command = skipFanout(messagePrefix, e);
    } else {
      command = skipExecution(messagePrefix, e);
    }
    return command;
  }

  @SuppressWarnings("rawtypes")
  static <T> DependencyCommand<T> _resolutionHelper(
      Set<? extends FacetSpec<?, ?>> sourceInputs,
      @Nullable Function<List<Errable<?>>, ?> oneToOneTransformer,
      @Nullable Function<List<Errable<?>>, ? extends Collection<?>> fanoutTransformer,
      List<? extends SkipPredicate<?>> skipPredicates,
      Facets facets) {
    boolean fanout = fanoutTransformer != null;
    List<Errable<?>> inputValues = new ArrayList<>();
    for (FacetSpec sourceInput : sourceInputs) {
      final Errable<?> inputValue;
      if (sourceInput instanceof One2OneDepSpec<?, ?, ?> depSpec) {
        inputValue = depSpec.getFacetValue(facets).response();
      } else if (sourceInput instanceof FanoutDepSpec<?, ?, ?> depSpec) {
        inputValue =
            Errable.withValue(
                depSpec.getFacetValue(facets).requestResponsePairs().stream()
                    .map(RequestResponse::response)
                    .filter(e -> e instanceof NonNil<?>)
                    .map(e -> (NonNil<?>) e)
                    .map(NonNil::value)
                    .toList());
      } else if (sourceInput instanceof DefaultFacetSpec mandatoryFacetDefaultSpec) {
        inputValue = mandatoryFacetDefaultSpec.getFacetValue(facets);
      } else {
        throw new UnsupportedOperationException("Unknown facet type " + sourceInput.getClass());
      }
      inputValues.add(inputValue);
    }

    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
      @SuppressWarnings("unchecked")
      MultiExecute<T> multiExecute =
          MultiExecute.executeFanoutWith(
              transformedInput.map(ts -> ((Collection<T>) ts).stream()).stream()
                  .flatMap(identity())
                  .collect(toImmutableList()));
      return multiExecute;
    } else {
      @SuppressWarnings("unchecked")
      SingleExecute<T> tSingleExecute =
          SingleExecute.executeWith((T) transformedInput.orElse(null));
      return tSingleExecute;
    }
  }

  public static <T, CV extends Request, DV extends Request> SimpleInputResolver toResolver(
      DependencySpec<?, CV, DV> dependency, SimpleInputResolverSpec<T, CV, DV> spec) {
    if (spec.canFanout()) {
      return new SimpleFanoutInputResolver<>(dependency, spec);
    } else {
      return new SimpleOne2OneInputResolver<>(dependency, spec);
    }
  }

  private InputResolverUtil() {}

  //  public static Comparator<QualifiedInputs> getQualifiedInputsComparator() {
  //    return comparingInt(QualifiedInputs::dependencyId)
  //        .thenComparingInt(q -> q.inputNames().size())
  //        .thenComparing(
  //            QualifiedInputs::inputNames,
  //            // Two resolvers resoving the same dependency cannot have a common inputName
  //            // So we can just compare the lexicographically first elements of both
  //            // inputNames to get a deterministic comparator
  //            comparing(strings -> strings.stream().sorted().findFirst().orElse("")));
  //  }
}
