package com.flipkart.krystal.lattice.ext.grpc.codegen;

import com.flipkart.krystal.lattice.ext.grpc.GrpcServerDopant;
import javax.lang.model.element.TypeElement;
import lombok.experimental.UtilityClass;

@UtilityClass
class LatticeGrpcUtils {
  public static final String APP_DOPANT_NAME_SEPARATOR = "_";
  public static final String DOPANT_IMPL_SUFFIX = "_Impl";

  static String getDopantImplName(TypeElement latticeAppElem) {
    return latticeAppElem.getSimpleName().toString()
        + APP_DOPANT_NAME_SEPARATOR
        + GrpcServerDopant.class.getSimpleName()
        + DOPANT_IMPL_SUFFIX;
  }
}
