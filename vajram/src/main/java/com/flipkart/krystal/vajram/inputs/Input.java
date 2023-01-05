package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static java.util.Arrays.stream;

import com.flipkart.krystal.datatypes.DataType;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.Builder;

@Builder
public record Input<T>(
    String name,
    DataType type,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    Set<InputSource> sources)
    implements VajramInputDefinition {

  private static final Set<InputSource> DEFAULT_INPUT_SOURCES = Set.of(InputSource.CLIENT);

  public boolean isOptional() {
    return !isMandatory();
  }

  public Set<InputSource> sources() {
    if (sources == null || sources.isEmpty()) {
      return DEFAULT_INPUT_SOURCES;
    }
    return sources;
  }

  public static class InputBuilder<T> {

    public InputBuilder<T> isMandatory() {
      return mandatory(true);
    }

    public InputBuilder<T> mandatory(boolean isMandatory) {
      this.isMandatory = isMandatory;
      return this;
    }

    public InputBuilder<T> needsModulation(boolean needsModulation) {
      this.needsModulation = needsModulation;
      return this;
    }

    public InputBuilder<T> needsModulation() {
      return needsModulation(true);
    }

    public InputBuilder<T> sources(InputSource... inputSources) {
      if (inputSources != null) {
        this.sources = newLinkedHashSet(stream(inputSources)::iterator);
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
