package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class IntegerType implements DataType<Integer> {

  private static final IntegerType INSTANCE = new IntegerType();

  public static IntegerType integer() {
    return INSTANCE;
  }

  private IntegerType() {}

  @Override
  public Optional<Type> javaReflectType() {
    return Optional.of(Integer.TYPE);
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(Integer.class.getName()).asType();
  }
}
