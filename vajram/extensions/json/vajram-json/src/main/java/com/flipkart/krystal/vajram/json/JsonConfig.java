package com.flipkart.krystal.vajram.json;

import static com.flipkart.krystal.vajram.json.JsonConfig.SerdeOutputType.BYTE_ARRAY;
import static java.lang.annotation.ElementType.TYPE_USE;

import com.google.auto.value.AutoAnnotation;
import java.lang.annotation.Target;
import lombok.experimental.UtilityClass;

@Target(TYPE_USE)
public @interface JsonConfig {
  SerdeOutputType serializeAs() default BYTE_ARRAY;

  enum SerdeOutputType {
    BYTE_ARRAY,
    STRING,
  }

  @UtilityClass
  final class Creator {
    public static @AutoAnnotation JsonConfig createDefault() {
      return new AutoAnnotation_JsonConfig_Creator_createDefault();
    }

    public static @AutoAnnotation JsonConfig create(SerdeOutputType serializeAs) {
      return new AutoAnnotation_JsonConfig_Creator_create(serializeAs);
    }
  }
}
