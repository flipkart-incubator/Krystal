package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.TypeName;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;

public record VajramInfo(
    VajramID vajramId,
    String packageName,
    ImmutableList<InputModel<?>> inputs,
    ImmutableList<DependencyModel> dependencies,
    TypeName responseType,
    TypeElement vajramClass) {

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(inputs.stream(), dependencies.stream());
  }
}
