package com.flipkart.krystal.vajram.codegen.common.datatypes;

import java.lang.reflect.Type;
import java.util.ServiceLoader;
import javax.annotation.processing.ProcessingEnvironment;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class DataTypeRegistry implements DataTypeFactory {

  public static final Iterable<DataTypeFactory> DATA_TYPE_FACTORIES =
      ServiceLoader.load(DataTypeFactory.class, DataTypeFactory.class.getClassLoader());

  private final DefaultDataTypeFactory defaultFactory;

  public DataTypeRegistry() {
    this.defaultFactory = new DefaultDataTypeFactory();
  }

  @Override
  public CodeGenType create(
      ProcessingEnvironment processingEnv,
      String canonicalClassName,
      CodeGenType... typeParameters) {
    for (DataTypeFactory factory : DATA_TYPE_FACTORIES) {
      @Nullable CodeGenType dataType =
          factory.create(processingEnv, canonicalClassName, typeParameters);
      if (dataType != null) {
        return dataType;
      }
    }
    return defaultFactory.create(processingEnv, canonicalClassName, typeParameters);
  }

  @Override
  public @Nullable CodeGenType create(ProcessingEnvironment processingEnv, Type type) {
    for (DataTypeFactory factory : DATA_TYPE_FACTORIES) {
      @Nullable CodeGenType dataType = factory.create(processingEnv, type);
      if (dataType != null) {
        return dataType;
      }
    }
    return defaultFactory.create(processingEnv, type);
  }
}
