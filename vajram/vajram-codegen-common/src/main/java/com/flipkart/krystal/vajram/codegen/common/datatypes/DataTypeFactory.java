package com.flipkart.krystal.vajram.codegen.common.datatypes;

import com.flipkart.krystal.datatypes.DataType;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataTypeFactory {
  @Nullable CodeGenDataType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenDataType... typeParameters);
}
