package com.flipkart.krystal.codegen.common.datatypes;

import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataTypeFactory {
  @Nullable CodeGenType create(
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnvironment);
}
