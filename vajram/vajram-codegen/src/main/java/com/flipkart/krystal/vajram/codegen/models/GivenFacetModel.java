package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.flipkart.krystal.vajram.facets.Mandatory;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.returnsreceiver.qual.This;

/**
 * Represents any face which is "given" to the vajram from outside - like client provided inputs and
 * platform provided injections
 */
@Builder
public record GivenFacetModel<T>(
    int id,
    @NonNull String name,
    @NonNull VajramInfoLite vajramInfo,
    @NonNull DataType<T> dataType,
    @Nullable Mandatory mandatoryAnno,
    @NonNull String documentation,
    boolean isBatched,
    ImmutableSet<FacetType> facetTypes,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<FacetType> ALLOWED_FACET_TYPES =
      ImmutableSet.copyOf(EnumSet.of(FacetType.INPUT, FacetType.INJECTION));

  public GivenFacetModel {
    if (!facetTypes.stream().allMatch(ALLOWED_FACET_TYPES::contains)) {
      throw new IllegalArgumentException(
          "Allowed Facet types: " + ALLOWED_FACET_TYPES + ". Found: " + facetTypes);
    }
  }

  public ImmutableSet<InputSource> sources() {
    ImmutableSet.Builder<InputSource> sources = ImmutableSet.builderWithExpectedSize(2);
    for (FacetType facetType : facetTypes) {
      switch (facetType) {
        case INPUT -> sources.add(InputSource.CLIENT);
        case INJECTION -> sources.add(InputSource.SESSION);
        default -> throw new IllegalStateException("Unexpected value: " + facetType);
      }
    }
    return sources.build();
  }

  @Override
  public boolean isGiven() {
    return true;
  }

  public static class GivenFacetModelBuilder<T> {

    public @This GivenFacetModelBuilder<T> facetTypes(EnumSet<FacetType> facetTypes) {
      if (facetTypes != null) {
        this.facetTypes = ImmutableSet.copyOf(facetTypes);
      }
      return this;
    }
  }
}
