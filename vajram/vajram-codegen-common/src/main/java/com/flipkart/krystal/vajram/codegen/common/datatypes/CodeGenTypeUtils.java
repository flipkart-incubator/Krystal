package com.flipkart.krystal.vajram.codegen.common.datatypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CodeGenTypeUtils {

  static TypeMirror box(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    if (typeMirror.getKind().isPrimitive()) {
      return processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType();
    } else {
      return typeMirror;
    }
  }
}
