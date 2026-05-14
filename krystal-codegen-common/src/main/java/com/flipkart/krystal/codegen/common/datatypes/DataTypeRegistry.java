package com.flipkart.krystal.codegen.common.datatypes;

import java.util.List;
import java.util.ServiceLoader;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
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
      TypeMirror typeMirror,
      List<CodeGenType> typeParameters,
      ProcessingEnvironment processingEnv) {
    for (DataTypeFactory factory : DATA_TYPE_FACTORIES) {
      @Nullable CodeGenType dataType = factory.create(typeMirror, typeParameters, processingEnv);
      if (dataType != null) {
        return dataType;
      }
    }
    return defaultFactory.create(typeMirror, typeParameters, processingEnv);
  }
}
