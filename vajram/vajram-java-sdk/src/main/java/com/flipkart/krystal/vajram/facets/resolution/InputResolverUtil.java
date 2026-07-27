package com.flipkart.krystal.vajram.facets.resolution;

import static com.flipkart.krystal.vajram.facets.FanoutCommand.executeFanoutWith;
import static com.flipkart.krystal.vajram.facets.FanoutCommand.skipFanout;
import static com.flipkart.krystal.vajram.facets.One2OneCommand.skipExecution;

import com.flipkart.krystal.data.ErrableFacetValue;
import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.DependencyCommand;
import com.flipkart.krystal.vajram.facets.FanoutCommand;
import com.flipkart.krystal.vajram.facets.One2OneCommand;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.FanoutTransformer;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.One2OneTransformer;
import com.flipkart.krystal.vajram.facets.specs.DependencySpec;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputResolverUtil {

  @SuppressWarnings("rawtypes")
  static <T> DependencyCommand<T> _resolutionHelper(
      @Nullable FacetSpec<?, ?> sourceFacet,
      Transformer transformer,
      List<? extends SkipPredicate> skipPredicates,
      FacetValues facetValues) {
    final FacetValue<?> sourceFacetValue =
        sourceFacet != null ? sourceFacet.getFacetValue(facetValues) : ErrableFacetValue.nil();

    if (sourceFacetValue != null) {
      SkipPredicate skipPredicate = null;
      for (SkipPredicate p : skipPredicates) {
        if (p.condition().test(sourceFacetValue)) {
          skipPredicate = p;
          break;
        }
      }
      if (skipPredicate != null) {
        if (transformer instanceof FanoutTransformer) {
          return skipFanout(skipPredicate.reason());
        } else {
          return skipExecution(skipPredicate.reason());
        }
      }
    }

    if (transformer instanceof FanoutTransformer fanoutTransformer) {
      Collection<?> transformedInput = fanoutTransformer.apply(sourceFacetValue);
      if (transformedInput == null) {
        return executeFanoutWith(ImmutableList.of());
      } else {
        @SuppressWarnings("unchecked")
        FanoutCommand<T> fanoutCommand = executeFanoutWith((Collection<T>) transformedInput);
        return fanoutCommand;
      }
    } else if (transformer instanceof One2OneTransformer one2OneTransformer) {
      @SuppressWarnings("unchecked")
      One2OneCommand<T> one2OneCommand =
          One2OneCommand.executeWith((T) one2OneTransformer.apply(sourceFacetValue));
      return one2OneCommand;
    } else {
      throw new AssertionError("Not possible.");
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
