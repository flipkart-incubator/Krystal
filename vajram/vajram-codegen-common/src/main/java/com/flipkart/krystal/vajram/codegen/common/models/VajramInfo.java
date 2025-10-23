package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.INPUT;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
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
        if (!defaultFacet.facetType().equals(INPUT)) {
          lite.util()
              .codegenUtil()
              .error("Only INPUT facets are supported in Traits", defaultFacet.facetField());
        }
      }
      if (!dependencies.isEmpty()) {
        lite.util()
            .codegenUtil()
            .error("Traits cannot have dependencies", dependencies.get(0).facetField());
      }
    }
  }

  public Stream<FacetGenModel> facetStream() {
    return Stream.concat(givenFacets.stream(), dependencies.stream());
  }

  public TypeElement vajramClassElem() {
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
                ClassName.get(Request.class),
                util().codegenUtil().toTypeName(lite.responseType()).box()));
  }

  private VajramCodeGenUtility util() {
    return lite.util();
  }

  public Iterable<TypeName> immutReqInterfaceSuperTypes() {
    return List.of(
        lite.requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.reqImmutInterfaceType()
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.class),
                util().codegenUtil().toTypeName(lite.responseType()).box()));
  }

  public Iterable<TypeName> reqBuilderInterfaceSuperTypes() {
    return List.of(
        lite.requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.reqImmutInterfaceType().nestedClass("Builder")
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.Builder.class),
                util().codegenUtil().toTypeName(lite.responseType()).box()));
  }

  public VajramInfoLite conformsToTraitOrSelf() {
    return conformsToTraitInfo == null ? lite : conformsToTraitInfo;
  }

  public ClassName facetsInterfaceType() {
    return ClassName.get(lite.packageName(), vajramName() + Constants.FACETS_CLASS_SUFFIX);
  }

  public ClassName facetsImmutPojoType() {
    return ClassName.get(lite.packageName(), vajramName() + Constants.FACETS_IMMUT_CLASS_SUFFIX);
  }
}
