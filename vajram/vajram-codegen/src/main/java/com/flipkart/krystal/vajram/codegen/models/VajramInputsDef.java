package com.flipkart.krystal.vajram.codegen.models;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.flipkart.krystal.vajram.inputs.VajramInputDefinition;
import com.google.common.collect.ImmutableList;
import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public record VajramInputsDef(
    ImmutableList<InputDef> inputs, ImmutableList<DependencyDef> dependencies) {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper(new YAMLFactory())
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

  @JsonCreator
  @ConstructorProperties({"inputs", "dependencies"})
  public VajramInputsDef(

      @JsonSetter(nulls = Nulls.AS_EMPTY) List<InputDef> inputs,
      @JsonSetter(nulls = Nulls.AS_EMPTY) List<DependencyDef> dependencies) {
    this(ImmutableList.copyOf(inputs), ImmutableList.copyOf(dependencies));
  }

  public static VajramInputsDef from(File file) throws IOException {
    try {
      return OBJECT_MAPPER.readValue(file, VajramInputsDef.class);
    } catch (IOException e) {
      throw new IOException("Error wile loading .vajram.yaml file: %s".formatted(file), e);
    }
  }

  public ImmutableList<VajramInputDefinition> allInputsDefinitions() {
    return Stream.concat(inputs.stream(), dependencies.stream())
        .map(AbstractInput::toInputDefinition)
        .collect(toImmutableList());
  }
}
