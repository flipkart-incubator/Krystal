package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;

@SuppressWarnings("Singleton")
public final class ObjectType implements DataType<Object> {
  private static final ObjectType INSTANCE = new ObjectType();

  public static ObjectType object() {
    return INSTANCE;
  }

  private ObjectType() {}

  @Override
  public Optional<Type> javaReflectType() {
    return Optional.of(Object.class);
  }

  @Override
  public TypeMirror javaModelType(ProcessingEnvironment processingEnv) {
    return processingEnv.getElementUtils().getTypeElement(Object.class.getName()).asType();
  }
}
