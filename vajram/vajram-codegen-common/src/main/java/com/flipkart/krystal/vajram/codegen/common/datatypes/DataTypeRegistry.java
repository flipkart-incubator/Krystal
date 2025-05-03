package com.flipkart.krystal.vajram.codegen.common.datatypes;

import com.flipkart.krystal.datatypes.DataType;
import com.flipkart.krystal.datatypes.JavaType;
import java.util.ServiceLoader;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DataTypeRegistry implements DataTypeFactory {

  private final DefaultDataTypeFactory defaultFactory;
  private final Iterable<DataTypeFactory> dataTypeFactories;

  public DataTypeRegistry() {
    this.defaultFactory = new DefaultDataTypeFactory();
    this.dataTypeFactories = ServiceLoader.load(DataTypeFactory.class);
  }

  @Override
  public CodeGenDataType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      DataType<?>... typeParameters) {
    for (DataTypeFactory factory : dataTypeFactories) {
      @Nullable CodeGenDataType dataType =
          factory.create(processingEnv, canonicalClassName, typeParameters);
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
