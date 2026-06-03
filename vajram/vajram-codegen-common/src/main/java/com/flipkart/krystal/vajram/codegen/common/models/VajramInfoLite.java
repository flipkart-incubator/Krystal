package com.flipkart.krystal.vajram.codegen.common.models;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.FacetType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import lombok.SneakyThrows;
import org.checkerframework.checker.nullness.qual.Nullable;

public record VajramInfoLite(
    VajramInputsInfo inputsInfo,
    TypeElement vajramOrReqClass,
    String docString,
    VajramCodeGenUtility util) {

  public VajramID vajramId() {
    return inputsInfo.vajramId();
  }

  public CodeGenType responseType() {
    return inputsInfo.responseType();
  }

  public List<? extends TypeMirror> typeArguments() {
    return inputsInfo.typeArguments();
  }

  public boolean isTrait() {
    return inputsInfo.isTrait();
  }

  public boolean isVajram() {
    return !isTrait();
  }

  public ClassName requestInterfaceClassName() {
    return inputsInfo.requestInterfaceClassName();
  }

  public TypeName requestInterfaceTypeName() {
    return inputsInfo.requestInterfaceTypeName();
  }

  public ClassName reqImmutInterfaceClassName() {
    return inputsInfo.reqImmutInterfaceClassName();
  }

  public TypeName reqImmutInterfaceTypeName() {
    return inputsInfo.reqImmutInterfaceTypeName();
  }

  public ClassName reqImmutPojoClassName() {
    return inputsInfo.reqImmutPojoClassName();
  }

  public TypeName reqImmutPojoTypeName() {
    return inputsInfo.reqImmutPojoTypeName();
  }

  public TypeName reqBuilderInterfaceType() {
    return inputsInfo.reqBuilderInterfaceType();
  }

  public CodeGenType responseTypeBounds() {
    return inputsInfo.responseTypeBounds();
  }

  @SneakyThrows
  public List<? extends AnnotationMirror> annotations() {
    return vajramOrReqClass.getAnnotationMirrors();
  }

  public record FacetDetail(
      String name, CodeGenType dataType, FacetType facetType, @Nullable String documentation) {

    public FacetDetail {}
  }
}
