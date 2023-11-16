package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.config.Tag;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.inputs.InputSource;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

@Builder
public record InputModel<T>(
    String name,
    DataType<?> type,
    ImmutableMap<String, Tag> tags,
    boolean isMandatory,
    T defaultValue,
    String documentation,
    boolean needsModulation,
    ImmutableSet<InputSource> sources)
    implements FacetGenModel {

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

    public InputModelBuilder<T> tags(Map<String, String> tags) {
      Map<String, Tag> tagsMap = new HashMap<>();
      tags.forEach((key, value) -> tagsMap.put(key, new Tag(key, value)));
      this.tags = ImmutableMap.copyOf(tagsMap);
      return this;
    }
  }
}
