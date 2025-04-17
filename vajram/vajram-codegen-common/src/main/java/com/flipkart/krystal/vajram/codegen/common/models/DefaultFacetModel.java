package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
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
public record DefaultFacetModel(
    int id,
    @NonNull String name,
    @NonNull VajramInfoLite vajramInfo,
    @NonNull DataType<Object> dataType,
    @Nullable String documentation,
    ImmutableSet<FacetType> facetTypes,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<FacetType> ALLOWED_FACET_TYPES =
      ImmutableSet.copyOf(EnumSet.of(FacetType.INPUT, FacetType.INJECTION));

  public DefaultFacetModel {
    if (!ALLOWED_FACET_TYPES.containsAll(facetTypes)) {
      throw new IllegalArgumentException(
          "Allowed Facet types: " + ALLOWED_FACET_TYPES + ". Found: " + facetTypes);
    }
  }

  @Override
  public boolean isGiven() {
    return true;
  }

  public static class DefaultFacetModelBuilder {

    public @This DefaultFacetModelBuilder facetTypes(EnumSet<FacetType> facetTypes) {
      if (facetTypes != null) {
        this.facetTypes = ImmutableSet.copyOf(facetTypes);
      }
      return this;
    }
  }
}
