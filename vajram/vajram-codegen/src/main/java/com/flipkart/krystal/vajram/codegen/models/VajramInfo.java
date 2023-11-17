package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;

public record VajramInfo(
    VajramID vajramId,
    String packageName,
    ImmutableList<InputModel<?>> inputs,
    ImmutableList<DependencyModel> dependencies,
    TypeElement vajramClass) {

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(inputs.stream(), dependencies.stream());
  }
}
