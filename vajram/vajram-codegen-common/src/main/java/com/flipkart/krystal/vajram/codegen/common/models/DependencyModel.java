package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.asClassName;
import static com.flipkart.krystal.facets.FacetType.DEPENDENCY;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.facets.FacetType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
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
    @NonNull TypeName depReqType,
    boolean canFanout,
    @Nullable String documentation,
    @NonNull VariableElement facetField)
    implements FacetGenModel {

  @Override
  public FacetType facetType() {
    return DEPENDENCY;
  }

  public ClassName depReqClassName() {
    return asClassName(depReqType());
  }

  public String depReqPackageName() {
    return depReqClassName().packageName();
  }
}
