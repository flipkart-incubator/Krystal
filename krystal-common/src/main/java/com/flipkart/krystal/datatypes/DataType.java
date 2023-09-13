package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;
import java.util.Optional;

public sealed interface DataType<T>
    permits BooleanType, CustomType, IntegerType, ListType, ObjectType, SetType, StringType {

  /**
   * @return The java {@link Type} corresponding to this data type. Or {@link Optional#empty()} if
   *     the type is not present, or not available to the current {@link ClassLoader}
   */
  Optional<Type> javaType();
}
