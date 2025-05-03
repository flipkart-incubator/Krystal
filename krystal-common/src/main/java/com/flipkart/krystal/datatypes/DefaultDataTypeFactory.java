package com.flipkart.krystal.datatypes;

import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class DefaultDataTypeFactory implements DataTypeFactory {

  @Override
  public <T> DataType<T> create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      DataType<?>... typeParameters) {
    return JavaType.create(canonicalClassName, typeParameters);
  }

  @Override
  public <T> DataType<T> create(Class<?> clazz, DataType<?>... typeParams) {
    return JavaType.create(clazz, typeParams);
  }
}
