package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import javax.lang.model.element.VariableElement;
import lombok.Builder;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Builder
public record DependencyModel(
    int id,
    @NonNull String name,
    @NonNull VajramInfoLite vajramInfo,
    @NonNull VajramInfoLite depVajramInfo,
    @NonNull CodeGenType dataType,
    @NonNull String depReqClassQualifiedName,
    boolean canFanout,
    @Nullable String documentation,
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
