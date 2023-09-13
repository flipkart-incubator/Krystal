package com.flipkart.krystal.vajram.inputs;

import com.flipkart.krystal.config.Tag;
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
    DataType<?> type,
    ImmutableMap<String, Tag> tags,
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

    public InputBuilder<T> tags(Map<String, String> tags) {
      Map<String, Tag> tagsMap = new HashMap<>();
      tags.forEach((key, value) -> tagsMap.put(key, new Tag(key, value)));
      this.tags = ImmutableMap.copyOf(tagsMap);
      return this;
    }
  }
}
