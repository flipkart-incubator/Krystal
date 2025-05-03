package com.flipkart.krystal.datatypes;

import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataTypeFactory {
  <T> @Nullable DataType<T> create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      DataType<?>... typeParameters);

  <T> @Nullable DataType<T> create(Class<?> clazz, DataType<?>... typeParams);
}
