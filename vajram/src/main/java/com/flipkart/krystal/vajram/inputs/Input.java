package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.Sets.newHashSet;

import com.flipkart.krystal.datatypes.DataType;
import java.util.Set;
import lombok.Builder;

@Builder
public record Input<T>(
    String name,
    DataType<? extends T> type,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    Set<InputSources> resolvableBy)
    implements VajramInputDefinition {

  private static final Set<InputSources> DEFAULT_INPUT_SOURCES = Set.of(InputSources.CLIENT);

  public boolean isOptional() {
    return !isMandatory();
  }

  @Override
  public Set<InputSources> resolvableBy() {
    if (resolvableBy == null || resolvableBy.isEmpty()) {
      return DEFAULT_INPUT_SOURCES;
    }
    return resolvableBy;
  }

  public static class InputBuilder<T> {

    public InputBuilder<T> isMandatory() {
      this.isMandatory = true;
      return this;
    }

    public InputBuilder<T> needsModulation() {
      this.needsModulation = true;
      return this;
    }

    public InputBuilder<T> resolvableBy(InputSources... inputSources) {
      this.resolvableBy = newHashSet(inputSources);
      return this;
    }
  }
}
