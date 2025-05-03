package com.flipkart.krystal.datatypes;

import java.util.ServiceLoader;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class DataTypeRegistry implements DataTypeFactory {

  private final DefaultDataTypeFactory defaultFactory;
  private final Iterable<DataTypeFactory> dataTypeFactories;

  public DataTypeRegistry() {
    this.defaultFactory = new DefaultDataTypeFactory();
    this.dataTypeFactories = ServiceLoader.load(DataTypeFactory.class);
  }

  @Override
  public <T> DataType<T> create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      DataType<?>... typeParameters) {
    for (DataTypeFactory factory : dataTypeFactories) {
      DataType<T> dataType = factory.create(processingEnv, canonicalClassName, typeParameters);
      if (dataType != null) {
        return dataType;
      }
    }
    return defaultFactory.create(processingEnv, canonicalClassName, typeParameters);
  }

  @Override
  public <T> JavaType<@NonNull T> create(Class<?> clazz, DataType<?>... typeParams) {
    return null;
  }
}
