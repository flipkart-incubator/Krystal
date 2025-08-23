package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents any face which is "given" to the vajram from outside - like client provided inputs and
 * platform provided injections
 */
@Builder
public record DefaultFacetModel(
    int id,
    @NonNull String name,
    @NonNull VajramInfoLite vajramInfo,
    @NonNull CodeGenType dataType,
    @Nullable String documentation,
    FacetType facetType,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<FacetType> ALLOWED_FACET_TYPES =
      ImmutableSet.copyOf(EnumSet.of(FacetType.INPUT, FacetType.INJECTION));

  public DefaultFacetModel {
    if (!ALLOWED_FACET_TYPES.contains(facetType)) {
      throw new IllegalArgumentException(
          "Allowed Facet types: " + ALLOWED_FACET_TYPES + ". Found: " + facetType);
    }
  }

  @Override
  public boolean isGiven() {
    return true;
  }
}
