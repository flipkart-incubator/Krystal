package com.flipkart.krystal.datatypes;

import static javax.lang.model.type.TypeKind.INT;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class IntegerType extends AbstractDataType<Integer> {

  private static final IntegerType INSTANCE = new IntegerType();

  public static IntegerType integer() {
    return INSTANCE;
  }

  private IntegerType() {}

  @Override
  public Class<Integer> javaReflectType() {
    return Integer.TYPE;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getTypeUtils().getPrimitiveType(INT);
  }
}
