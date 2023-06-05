package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.config.LogicTag;
import com.flipkart.krystal.datatypes.DataType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder
public record Input<T>(
    String name,
    DataType type,
    ImmutableMap<String, LogicTag> inputTags,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    ImmutableSet<InputSource> sources)
    implements VajramInputDefinition {

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

    public InputBuilder<T> inputTags(Map<String, String> inputTags) {
      Map<String, LogicTag> inputTagsMap = new HashMap<>();
      inputTags.forEach((key, value) -> inputTagsMap.put(key, new LogicTag(key, value)));
      this.inputTags = ImmutableMap.copyOf(inputTagsMap);
      return this;
    }
  }
}
