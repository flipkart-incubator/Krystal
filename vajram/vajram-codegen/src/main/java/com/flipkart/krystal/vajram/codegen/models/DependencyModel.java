package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;

@Builder
public record DependencyModel(
    int id,
    @NonNull String name,
    @NonNull VajramInfoLite depVajramInfoLite,
    @NonNull DataType<?> dataType,
    @NonNull String depReqClassQualifiedName,
    boolean isMandatory,
    boolean canFanout,
    @NonNull String documentation,
    boolean isBatched,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<FacetType> DEP_FACET_TYPE =
      ImmutableSet.copyOf(EnumSet.of(FacetType.DEPENDENCY));

  @Override
  public ImmutableSet<FacetType> facetTypes() {
    return DEP_FACET_TYPE;
  }

  public String depReqPackageName() {
    return depReqClassQualifiedName.substring(0, depReqClassQualifiedName.lastIndexOf('.'));
  }
}
