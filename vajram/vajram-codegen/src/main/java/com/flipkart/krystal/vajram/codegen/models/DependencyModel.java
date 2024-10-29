package com.flipkart.krystal.vajram.codegen.models;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.VajramID;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;

@Builder
public record DependencyModel(
    int id,
    String name,
    VajramID depVajramId,
    DataType<?> dataType,
    String depReqClassQualifiedName,
    boolean isMandatory,
    boolean canFanout,
    String documentation,
    boolean isBatched,
    VariableElement facetField)
    implements FacetGenModel {

  private static final ImmutableSet<FacetType> DEP_FACET_TYPE =
      ImmutableSet.copyOf(EnumSet.of(FacetType.DEPENDENCY));

  @Override
  public ImmutableSet<FacetType> facetTypes() {
    return DEP_FACET_TYPE;
  }
}
