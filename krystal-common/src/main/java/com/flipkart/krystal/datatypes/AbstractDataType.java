package com.flipkart.krystal.datatypes;

abstract sealed class AbstractDataType<T> implements DataType<T>
    permits BooleanType, CustomType, IntegerType, ListType, SetType, StringType {
  @Override
  public String toString() {
    try {
      return javaReflectType().toString();
    } catch (ClassNotFoundException e) {
      return super.toString();
    }
  }
}
