package com.flipkart.krystal.vajram.facets;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.tags.ElementTags;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Function;
import lombok.Builder;

@Builder
public record InputDef<T>(
    int id,
    String name,
    DataType<?> type,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean isBatched,
    ImmutableSet<InputSource> sources,
    ElementTags tags,
    FacetValueGetter getter,
    FacetValueSetter setter)
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

  public static class InputDefBuilder<T> {

    public InputDefBuilder<T> sources(InputSource... inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }

    public InputDefBuilder<T> sources(Set<InputSource> inputSources) {
      if (inputSources != null) {
        this.sources = ImmutableSet.copyOf(inputSources);
      }
      return this;
    }
  }
}
