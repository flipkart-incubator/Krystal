package com.flipkart.krystal.model;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class Validations {
  public static <T> T validateMandatory(@Nullable T t, String typeName, String dataFieldName) {
    if (t == null) {
      throw new MandatoryFieldMissingException(typeName, dataFieldName);
    }
    return t;
  }

  public static <T> T getOrDefault(@Nullable T t, T defaultValue) {
    if (t == null) {
      return defaultValue;
    }
    return t;
  }
}
