package com.flipkart.krystal.vajram.codegen;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;

public record VajramInfo(
    VajramInfoLite lite,
    ImmutableList<GivenFacetModel> givenFacets,
    ImmutableList<DependencyModel> dependencies,
    TypeElement vajramClass) {

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(givenFacets.stream(), dependencies.stream());
  }
}
