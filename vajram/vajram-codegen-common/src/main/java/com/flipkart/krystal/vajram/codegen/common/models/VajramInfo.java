package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.INPUT;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

public record VajramInfo(
    VajramInfoLite lite,
    ImmutableList<DefaultFacetModel> givenFacets,
    ImmutableList<DependencyModel> dependencies,
    @Nullable VajramInfoLite conformsToTraitInfo) {

  public VajramInfo {
    if (lite.isTrait()) {
      for (DefaultFacetModel defaultFacet : givenFacets) {
        if (!defaultFacet.facetTypes().equals(Set.of(INPUT))) {
          lite.util().error("Only INPUT facets are supported in Traits", defaultFacet.facetField());
        }
      }
      if (!dependencies.isEmpty()) {
        lite.util().error("Traits cannot have dependencies", dependencies.get(0).facetField());
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

  public Iterable<TypeName> requestInterfaceSuperTypes() {
    return List.of(
        conformsToTraitInfo != null
            ? conformsToTraitInfo.requestInterfaceType()
            : ParameterizedTypeName.get(
                ClassName.get(Request.class), lite.util().toTypeName(lite.responseType()).box()));
  }

  public Iterable<TypeName> immutReqInterfaceSuperTypes() {
    return List.of(
        lite.requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.immutReqInterfaceType()
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.class),
                lite.util().toTypeName(lite.responseType()).box()));
  }

  public Iterable<TypeName> reqBuilderInterfaceSuperTypes() {
    return List.of(
        lite.requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.immutReqInterfaceType().nestedClass("Builder")
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.Builder.class),
                lite.util().toTypeName(lite.responseType()).box()));
  }

  public VajramInfoLite conformsToTraitOrSelf() {
    return conformsToTraitInfo == null ? lite : conformsToTraitInfo;
  }
}
