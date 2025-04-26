package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  @SuppressWarnings("rawtypes")
  static <T> DependencyCommand<T> _resolutionHelper(
      @Nullable FacetSpec<?, ?> sourceFacet,
      Transformer transformer,
      List<? extends SkipPredicate> skipPredicates,
      FacetValues facetValues) {
    final FacetValue<?> sourceFacetValue =
        sourceFacet != null ? sourceFacet.getFacetValue(facetValues) : Errable.nil();

    if (sourceFacetValue != null) {
      @SuppressWarnings("unchecked")
      Optional<SkipPredicate> skipPredicate =
          skipPredicates.stream()
              .map(p -> (SkipPredicate) p)
              .filter(sSkipPredicate -> sSkipPredicate.condition().test(sourceFacetValue))
              .findFirst();
      if (skipPredicate.isPresent()) {
        if (transformer.canFanout()) {
          return skipFanout(skipPredicate.get().reason());
        } else {
          return skipExecution(skipPredicate.get().reason());
        }
      }
    }

    Optional<Object> transformedInput = ofNullable(transformer.apply(sourceFacetValue));
    if (transformer.canFanout()) {
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
