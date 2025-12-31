package com.flipkart.krystal.lattice.ext.dropwizard.codegen;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.squareup.javapoet.ClassName;
import lombok.experimental.UtilityClass;

@UtilityClass
class DwCodegenUtil {

  static ClassName getDwAppClassName(LatticeCodegenContext context) {
    return ClassName.get(
        context
            .codeGenUtility()
            .codegenUtil()
            .processingEnv()
            .getElementUtils()
            .getPackageOf(context.latticeAppTypeElement())
            .getQualifiedName()
            .toString(),
        context.latticeAppTypeElement().getSimpleName().toString() + "_DWApp");
  }
}
