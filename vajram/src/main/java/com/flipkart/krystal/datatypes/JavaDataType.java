package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

interface JavaDataType<T> extends DataType {

  /**
   * @return The java {@link Type} corresponding to this data type. Or {@link Optional#empty()} if
   *     the type is not present, or not available to the current {@link ClassLoader}
   */
  Optional<Type> javaType();
}
