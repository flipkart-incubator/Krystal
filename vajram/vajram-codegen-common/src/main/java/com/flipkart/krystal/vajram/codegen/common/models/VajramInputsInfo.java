package com.flipkart.krystal.vajram.codegen.common.models;

import static com.flipkart.krystal.codegen.common.models.CodeGenUtility.asTypeNameWithTypes;
import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getImmutRequestInterfaceName;
import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getImmutRequestPojoName;
import static com.flipkart.krystal.vajram.codegen.common.models.VajramCodeGenUtility.getRequestInterfaceName;

import com.flipkart.krystal.codegen.common.datatypes.CodeGenType;
import com.flipkart.krystal.codegen.common.datatypes.VariableCodeGenType;
import com.flipkart.krystal.core.VajramID;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.List;
import javax.lang.model.type.TypeMirror;

/**
 * Encapsulates the information about vajram inputs needed for request interface generation. This
 * can be used both for vajrams that define inputs inside their class and for vajrams that define
 * inputs externally via {@link com.flipkart.krystal.facets.InputsForVajram}.
 */
public record VajramInputsInfo(
    VajramID vajramId,
    CodeGenType responseType,
    String requestPackageName,
    List<DefaultFacetModel> inputs,
    List<? extends TypeMirror> typeArguments,
    boolean isTrait) {

  public String vajramName() {
    return vajramId.id();
  }

  public ClassName requestInterfaceClassName() {
    return ClassName.get(requestPackageName(), getRequestInterfaceName(vajramId().id()));
  }

  public TypeName requestInterfaceTypeName() {
    return asTypeNameWithTypes(requestInterfaceClassName(), typeArguments());
  }

  public ClassName reqImmutInterfaceClassName() {
    return ClassName.get(requestPackageName(), getImmutRequestInterfaceName(vajramId().id()));
  }

  public TypeName reqImmutInterfaceTypeName() {
    return asTypeNameWithTypes(
        ClassName.get(requestPackageName(), getImmutRequestInterfaceName(vajramId().id())),
        typeArguments());
  }

  public ClassName reqImmutPojoClassName() {
    return ClassName.get(requestPackageName(), getImmutRequestPojoName(vajramId().id()));
  }

  public TypeName reqImmutPojoTypeName() {
    return asTypeNameWithTypes(reqImmutPojoClassName(), typeArguments());
  }

  public TypeName reqBuilderInterfaceType() {
    return asTypeNameWithTypes(
        reqImmutInterfaceClassName().nestedClass("Builder"), typeArguments());
  }

  public CodeGenType responseTypeBounds() {
    if (responseType instanceof VariableCodeGenType vct) {
      return vct.upperBound();
    }
    return responseType;
  }
}
