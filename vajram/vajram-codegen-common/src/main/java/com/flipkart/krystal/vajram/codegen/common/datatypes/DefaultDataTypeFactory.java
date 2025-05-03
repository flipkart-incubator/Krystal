package com.flipkart.krystal.vajram.codegen.common.datatypes;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import javax.annotation.processing.ProcessingEnvironment;

public final class DefaultDataTypeFactory implements DataTypeFactory {

  @Override
  public CodeGenDataType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenDataType... typeParameters) {
    return new CodeGenJavaType(canonicalClassName, ImmutableList.copyOf(typeParameters));
  }
}
