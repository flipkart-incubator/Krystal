package com.flipkart.krystal.datatypes;

import static javax.lang.model.type.TypeKind.BOOLEAN;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class BooleanType extends AbstractDataType<Boolean> {
  private static final BooleanType INSTANCE = new BooleanType();

  public static BooleanType bool() {
    return INSTANCE;
  }

  private BooleanType() {}

  @Override
  public Class<Boolean> javaReflectType() {
    return Boolean.TYPE;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getTypeUtils().getPrimitiveType(BOOLEAN);
  }
}
