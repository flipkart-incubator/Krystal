package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.Sets.newHashSet;

import com.flipkart.krystal.datatypes.DataType;
import java.util.Set;
import lombok.Builder;

@Builder
public record Input<T>(
    String name,
    DataType<? extends T> type,
    boolean mandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    Set<ResolutionSources> resolvableBy)
    implements VajramInputDefinition {

  private static final Set<ResolutionSources> DEFAULT_RESOLUTION_SOURCES =
      Set.of(ResolutionSources.REQUEST);

  public boolean isOptional() {
    return !mandatory();
  }

  @Override
  public Set<ResolutionSources> resolvableBy() {
    if (resolvableBy == null || resolvableBy.isEmpty()) {
      return DEFAULT_RESOLUTION_SOURCES;
    }
    return resolvableBy;
  }

  public static InputBuilder<String> string() {
    return builder();
  }

  public static class InputBuilder<T> {

    public InputBuilder<T> mandatory() {
      this.mandatory = true;
      return this;
    }

    public InputBuilder<T> needsModulation() {
      this.needsModulation = true;
      return this;
    }

    public InputBuilder<T> resolvableBy(ResolutionSources... resolutionSources) {
      this.resolvableBy = newHashSet(resolutionSources);
      return this;
    }
  }
}
