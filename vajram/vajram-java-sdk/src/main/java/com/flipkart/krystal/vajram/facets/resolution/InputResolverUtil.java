package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.NonNil;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponse;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
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

public final class InputResolverUtil {

  public static ResolverCommand toResolverCommand(
      DependencyCommand<ImmutableRequest.Builder> dependencyCommand) {
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
      Transformer transformer,
      List<? extends SkipPredicate> skipPredicates,
      FacetValues facetValues) {
    boolean fanout = transformer instanceof Transformer.Fanout;
    List<Errable<?>> inputValues = new ArrayList<>();
    for (FacetSpec sourceInput : sourceInputs) {
      final Errable<?> inputValue;
      if (sourceInput instanceof One2OneDepSpec<?, ?, ?> depSpec) {
        inputValue = depSpec.getFacetValue(facetValues).response();
      } else if (sourceInput instanceof FanoutDepSpec<?, ?, ?> depSpec) {
        inputValue =
            Errable.withValue(
                depSpec.getFacetValue(facetValues).requestResponsePairs().stream()
                    .map(RequestResponse::response)
                    .filter(e -> e instanceof NonNil<?>)
                    .map(e -> (NonNil<?>) e)
                    .map(NonNil::value)
                    .toList());
      } else if (sourceInput instanceof DefaultFacetSpec defaultSpec) {
        inputValue = defaultSpec.getFacetValue(facetValues);
      } else {
        throw new UnsupportedOperationException("Unknown facet type " + sourceInput.getClass());
      }
      inputValues.add(inputValue);
    }

    @SuppressWarnings("unchecked")
    Optional<SkipPredicate> skipPredicate =
        skipPredicates.stream()
            .map(p -> (SkipPredicate) p)
            .filter(sSkipPredicate -> sSkipPredicate.condition().test(inputValues))
            .findFirst();
    if (skipPredicate.isPresent()) {
      if (fanout) {
        return skipFanout(skipPredicate.get().reason());
      } else {
        return skipExecution(skipPredicate.get().reason());
      }
    }

    Optional<Object> transformedInput = ofNullable(transformer.apply(inputValues));
    if (fanout) {
      @SuppressWarnings("unchecked")
      FanoutCommand<T> fanoutCommand =
          FanoutCommand.executeFanoutWith(
              transformedInput.map(ts -> ((Collection<T>) ts).stream()).stream()
                  .flatMap(identity())
                  .collect(toImmutableList()));
      return fanoutCommand;
    } else {
      @SuppressWarnings("unchecked")
      One2OneCommand<T> tOne2OneCommand =
          One2OneCommand.executeWith((T) transformedInput.orElse(null));
      return tOne2OneCommand;
    }
  }

  public static <T, CV extends Request<?>, DV extends Request<T>> SimpleInputResolver toResolver(
      DependencySpec<T, CV, DV> dependency, SimpleInputResolverSpec<?, CV, DV> spec) {
    if (spec.canFanout()) {
      return new SimpleFanoutInputResolver<>(dependency, spec);
    } else {
      return new SimpleOne2OneInputResolver<>(dependency, spec);
    }
  }

  private InputResolverUtil() {}
}
