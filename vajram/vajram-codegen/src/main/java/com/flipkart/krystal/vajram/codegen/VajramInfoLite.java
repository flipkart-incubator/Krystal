package com.flipkart.krystal.vajram.codegen;

import static com.flipkart.krystal.vajram.codegen.Utils.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.Utils.getRequestInterfaceName;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableBiMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public record VajramInfoLite(
    VajramID vajramId,
    DataType<?> responseType,
    String packageName,
    ImmutableBiMap<Integer, String> facetIdNameMapping,
    @Nullable VajramInfoLite conformsToTraitInfo,
    Utils util) {

  public ClassName requestInterfaceType() {
    return ClassName.get(packageName(), getRequestInterfaceName(vajramId().vajramId()));
  }

  public ClassName immutReqInterfaceType() {
    return ClassName.get(packageName(), getImmutRequestInterfaceName(vajramId().vajramId()));
  }

  public TypeName builderInterfaceType() {
    return immutReqInterfaceType().nestedClass("Builder");
  }

  public Iterable<TypeName> requestInterfaceSuperTypes() {
    return List.of(
        conformsToTraitInfo != null
            ? conformsToTraitInfo.requestInterfaceType()
            : ParameterizedTypeName.get(
                ClassName.get(Request.class), util.toTypeName(responseType()).box()));
  }

  public Iterable<TypeName> immutReqInterfaceSuperTypes() {
    return List.of(
        requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.immutReqInterfaceType()
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.class), util.toTypeName(responseType()).box()));
  }

  public Iterable<TypeName> reqBuilderInterfaceSuperTypes() {
    return List.of(
        requestInterfaceType(),
        conformsToTraitInfo != null
            ? conformsToTraitInfo.immutReqInterfaceType().nestedClass("Builder")
            : ParameterizedTypeName.get(
                ClassName.get(ImmutableRequest.Builder.class),
                util.toTypeName(responseType()).box()));
  }

  public VajramInfoLite conformsToTraitOrSelf() {
    return conformsToTraitInfo == null ? this : conformsToTraitInfo;
  }
}
