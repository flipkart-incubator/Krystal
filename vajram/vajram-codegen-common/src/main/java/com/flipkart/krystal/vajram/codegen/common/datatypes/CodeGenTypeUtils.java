package com.flipkart.krystal.vajram.codegen.common.datatypes;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CodeGenTypeUtils {
  static final Map<String, StandardTypeInfo> nativeTypeInfos = new LinkedHashMap<>();

  static {
    for (StandardTypeInfo standardTypeInfo : StandardTypeInfo.values()) {
      for (String canonicalClassName : standardTypeInfo.canonicalClassNames()) {
        nativeTypeInfos.put(canonicalClassName, standardTypeInfo);
      }
    }
  }

  static TypeMirror box(TypeMirror typeMirror, ProcessingEnvironment processingEnv) {
    if (typeMirror.getKind().isPrimitive()) {
      return processingEnv.getTypeUtils().boxedClass((PrimitiveType) typeMirror).asType();
    } else {
      return typeMirror;
    }
  }
}
