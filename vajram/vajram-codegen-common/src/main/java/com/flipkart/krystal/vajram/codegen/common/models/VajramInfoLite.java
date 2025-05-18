package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getImmutRequestPojoName;
import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getRequestInterfaceName;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.vajram.Trait;
import com.google.common.collect.ImmutableBiMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import lombok.SneakyThrows;

public record VajramInfoLite(
    VajramID vajramId,
    CodeGenType responseType,
    String packageName,
    ImmutableBiMap<Integer, String> facetIdNameMapping,
    TypeElement vajramOrReqClass,
    String docString,
    VajramCodeGenUtility util) {

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
