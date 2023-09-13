package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

@SuppressWarnings("Singleton")
public final class ObjectType implements DataType<Object> {
  private static final ObjectType INSTANCE = new ObjectType();

  public static ObjectType object() {
    return INSTANCE;
  }

  private ObjectType() {}

  @Override
  public Optional<Type> javaType() {
    return Optional.of(Object.class);
  }
}
