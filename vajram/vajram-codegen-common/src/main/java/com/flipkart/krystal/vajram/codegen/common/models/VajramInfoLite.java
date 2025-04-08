package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getImmutRequestPojoName;
import static com.flipkart.krystal.vajram.codegen.common.models.Utils.getRequestInterfaceName;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.vajram.Trait;
import com.google.common.collect.ImmutableBiMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public record VajramInfoLite(
    VajramID vajramId,
    DataType<?> responseType,
    String packageName,
    ImmutableBiMap<Integer, String> facetIdNameMapping,
    @Nullable VajramInfoLite conformsToTraitInfo,
    TypeElement vajramOrReqClass,
    Utils util) {

  public ClassName requestInterfaceType() {
    return ClassName.get(packageName(), getRequestInterfaceName(vajramId().id()));
  }

  public ClassName immutReqInterfaceType() {
    return ClassName.get(packageName(), getImmutRequestInterfaceName(vajramId().id()));
  }

  public ClassName immutReqPojoType() {
    return ClassName.get(packageName(), getImmutRequestPojoName(vajramId().id()));
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

  @SneakyThrows
  public List<? extends AnnotationMirror> annotations() {
    return vajramOrReqClass.getAnnotationMirrors();
  }

  public boolean isTrait() {
    return vajramOrReqClass.getAnnotation(Trait.class) != null;
  }

  public boolean isVajram() {
    return !isTrait();
  }
}
