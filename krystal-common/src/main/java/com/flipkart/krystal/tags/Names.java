package com.flipkart.krystal.tags;

import com.google.auto.value.AutoAnnotation;
import jakarta.inject.Named;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Names {
  public static @AutoAnnotation Named named(String value) {
    return new AutoAnnotation_Names_named(value);
  }
}
