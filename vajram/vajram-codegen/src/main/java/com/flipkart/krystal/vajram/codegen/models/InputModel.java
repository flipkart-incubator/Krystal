package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.common.returnsreceiver.qual.This;

@Builder
public record InputModel<T>(
    int id,
    @NonNull String name,
    @NonNull DataType<T> dataType,
    boolean isMandatory,
    @NonNull String documentation,
    boolean isBatched,
    ImmutableSet<FacetType> facetTypes,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

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

  public static class InputModelBuilder<T> {

    public @This InputModelBuilder<T> facetTypes(EnumSet<FacetType> facetTypes) {
      if (facetTypes != null) {
        this.facetTypes = ImmutableSet.copyOf(facetTypes);
      }
      return this;
    }
  }
}
