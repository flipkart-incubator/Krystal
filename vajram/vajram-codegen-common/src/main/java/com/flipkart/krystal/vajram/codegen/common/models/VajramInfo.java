package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.INPUT;

import com.google.common.collect.ImmutableList;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;

public record VajramInfo(
    VajramInfoLite lite,
    ImmutableList<DefaultFacetModel> givenFacets,
    ImmutableList<DependencyModel> dependencies) {

  public VajramInfo {
    if (lite.isTrait()) {
      for (DefaultFacetModel defaultFacet : givenFacets) {
        if (!defaultFacet.facetTypes().equals(Set.of(INPUT))) {
          throw lite.util()
              .errorAndThrow(
                  "Only INPUT facets are supported in Traits", defaultFacet.facetField());
        }
      }
      if (!dependencies.isEmpty()) {
        throw lite.util()
            .errorAndThrow("Traits cannot have dependencies", dependencies.get(0).facetField());
      }
    }
  }

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(givenFacets.stream(), dependencies.stream());
  }

  public TypeElement vajramClass() {
    return lite.vajramOrReqClass();
  }

  public String vajramName() {
    return lite().vajramId().id();
  }
}
