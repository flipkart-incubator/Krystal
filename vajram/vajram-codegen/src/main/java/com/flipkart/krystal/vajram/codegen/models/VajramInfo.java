package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;

public record VajramInfo(
    VajramID vajramId,
    DataType<?> responseType,
    String packageName,
    ImmutableList<GivenFacetModel<?>> givenFacets,
    ImmutableList<DependencyModel> dependencies,
    ImmutableBiMap<String, Integer> facetIdsByName,
    TypeElement vajramClass) {

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(givenFacets.stream(), dependencies.stream());
  }
}
