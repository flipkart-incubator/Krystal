package com.flipkart.krystal.datatypes;

import java.lang.reflect.Type;

abstract class AbstractDataType<T> implements DataType<T> {
  abstract Type javaType();
}
