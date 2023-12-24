package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.datatypes.DataType;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Builder;

@Builder
public record Input<T>(
    String name,
    DataType<?> type,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    ImmutableSet<InputSource> sources)
    implements VajramFacetDefinition {

  private static final ImmutableSet<InputSource> DEFAULT_INPUT_SOURCES =
      ImmutableSet.of(InputSource.CLIENT);

  public boolean isOptional() {
    return !isMandatory();
  }

  public ImmutableSet<InputSource> sources() {
    if (sources == null || sources.isEmpty()) {
      return DEFAULT_INPUT_SOURCES;
    }
    return sources;
  }

  public static class InputBuilder<T> {

    public InputBuilder<T> sources(InputSource... inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }

    public InputBuilder<T> sources(Set<InputSource> inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }
  }
}
