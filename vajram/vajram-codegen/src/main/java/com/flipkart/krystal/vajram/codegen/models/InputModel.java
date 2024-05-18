package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.facets.InputSource;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record InputModel<T>(
    @NonNull String name,
    @NonNull DataType<?> type,
    boolean isMandatory,
    @NonNull String documentation,
    boolean isBatched,
    ImmutableSet<InputSource> sources,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<InputSource> DEFAULT_INPUT_SOURCES =
      ImmutableSet.of(InputSource.CLIENT);

  public ImmutableSet<InputSource> sources() {
    if (sources == null || sources.isEmpty()) {
      return DEFAULT_INPUT_SOURCES;
    }
    return sources;
  }

  public static class InputModelBuilder<T> {

    public InputModelBuilder<T> sources(InputSource... inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }

    public InputModelBuilder<T> sources(Set<InputSource> inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }
  }
}
