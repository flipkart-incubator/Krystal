package com.flipkart.krystal.codegen.common.datatypes;

import java.lang.reflect.Type;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataTypeFactory {
  @Nullable CodeGenType create(
      ProcessingEnvironment processingEnvironment,
      String canonicalClassName,
      CodeGenType... typeParameters);

  @Nullable CodeGenType create(ProcessingEnvironment processingEnvironment, Type type);
}
