package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class BooleanType implements DataType<Boolean> {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}

  @Override
  public Optional<Type> javaReflectType() {
    return Optional.of(Boolean.TYPE);
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(Boolean.class.getName()).asType();
  }
}
