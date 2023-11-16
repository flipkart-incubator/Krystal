package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class StringType implements DataType<String> {

  private static final StringType INSTANCE = new StringType();

  public static StringType string() {
    return INSTANCE;
  }

  private StringType() {}

  @Override
  public Optional<Type> javaReflectType() {
    return Optional.of(String.class);
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(String.class.getName()).asType();
  }
}
