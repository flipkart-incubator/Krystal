package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.VajramRequest;
import com.flipkart.krystal.vajram.facets.VajramFacetSpec;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The specification of a resolver which resolves exactly one input of a dependency.
 *
 * @param <T> The type of the dependency's input which is being resolved.
 * @param <CV> The type of the vajram doing the resolution.
 * @param <DV> The type of the vajram whose input is being resolved.
 */
@Getter
@Accessors(fluent = true)
public final class SimpleInputResolverSpec<
    T, CV extends VajramRequest<?>, DV extends VajramRequest<?>> {

  private final VajramFacetSpec<T, DV> targetInput;
  private final List<VajramFacetSpec<?, CV>> sourceInputs;
  private final List<SkipPredicate<?>> skipConditions;
  private final @Nullable Function<List<ValueOrError<?>>, @Nullable T> transformer;

  private final @Nullable Function<List<ValueOrError<?>>, ? extends Collection<? extends T>>
      fanoutTransformer;

  public SimpleInputResolverSpec(
      VajramFacetSpec<T, DV> targetInput,
      List<VajramFacetSpec<?, CV>> sourceInput,
      List<SkipPredicate<?>> skipConditions,
      @Nullable Function<List<ValueOrError<?>>, @Nullable T> transformer,
      @Nullable Function<List<ValueOrError<?>>, ? extends Collection<? extends T>>
          fanoutTransformer) {
    this.targetInput = targetInput;
    this.sourceInputs = sourceInput;
    this.skipConditions = skipConditions;
    this.transformer = transformer;
    this.fanoutTransformer = fanoutTransformer;
  }
}
