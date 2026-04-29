package com.flipkart.krystal.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ModelUtils {
  public static <T> T validateMandatory(T t, String fieldName, Class<?> clazz) {
    if (t == null) {
      throw new MandatoryFieldMissingException(clazz.getTypeName(), fieldName);
    }
    return t;
  }
}
