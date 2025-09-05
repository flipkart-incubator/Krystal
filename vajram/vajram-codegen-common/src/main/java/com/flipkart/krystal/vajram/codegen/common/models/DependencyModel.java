package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.facets.FacetType;
import com.flipkart.krystal.vajram.codegen.common.datatypes.CodeGenType;
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

  @Override
  public FacetType facetType() {
    return DEPENDENCY;
  }

  public String depReqPackageName() {
    return depReqClassQualifiedName.substring(0, depReqClassQualifiedName.lastIndexOf('.'));
  }
}
