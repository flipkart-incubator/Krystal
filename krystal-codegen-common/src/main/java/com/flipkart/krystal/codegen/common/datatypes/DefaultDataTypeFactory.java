package com.flipkart.krystal.codegen.common.datatypes;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

public final class DefaultDataTypeFactory implements DataTypeFactory {

  @Override
  public CodeGenType create(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnvironment) {
    CodeGenType standardJavaType =
        AnnotatedStandardJavaType.from(typeMirror, typeParameters, processingEnvironment);
    if (standardJavaType != null) {
      return standardJavaType;
    }
    return JavaCodeGenType.create(typeMirror, typeParameters, processingEnvironment);
  }
}
