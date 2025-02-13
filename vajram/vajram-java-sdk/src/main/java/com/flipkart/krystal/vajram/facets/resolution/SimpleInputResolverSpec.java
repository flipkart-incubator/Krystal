package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.Fanout;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;
import com.flipkart.krystal.vajram.facets.specs.InputMirrorSpec;
import com.google.common.collect.ImmutableSet;
import java.util.List;

/**
 * The specification of a resolver which resolves exactly one input of a dependency.
 *
 * @param <T> The type of the dependency's input which is being resolved.
 * @param <CV> The type of the vajram doing the resolution.
 * @param <DV> The type of the vajram whose input is being resolved.
 */
public record SimpleInputResolverSpec<T, CV extends Request, DV extends Request>(
    InputMirrorSpec<T, DV> targetInput,
    ImmutableSet<FacetSpec<?, CV>> sources,
    List<SkipPredicate> skipConditions,
    Transformer transformer) {

  public boolean canFanout() {
    return transformer instanceof Fanout;
  }
}
