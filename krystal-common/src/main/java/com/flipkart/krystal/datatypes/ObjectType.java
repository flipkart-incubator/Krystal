package com.flipkart.krystal.datatypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class ObjectType extends AbstractDataType<Object> {
  private static final ObjectType INSTANCE = new ObjectType();

  public static ObjectType object() {
    return INSTANCE;
  }

  private ObjectType() {}

  @Override
  public Class<Object> javaReflectType() {
    return Object.class;
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(Object.class.getName()).asType();
  }
}
