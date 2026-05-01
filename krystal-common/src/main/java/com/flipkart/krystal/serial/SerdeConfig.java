package com.flipkart.krystal.serial;

import static java.lang.annotation.ElementType.METHOD;

import com.flipkart.krystal.serial.SerdeConfig.SerdeConfigs;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Target(METHOD)
@Repeatable(SerdeConfigs.class)
public @interface SerdeConfig {
  Class<? extends SerdeProtocol> protocol();

  /**
   * Custom content types for this serde protocol. If not specified, the {@link
   * SerdeProtocol#defaultContentType()} of the serde protocol is used.
   */
  String[] contentTypes() default {};

  @Target(METHOD)
  @interface SerdeConfigs {
    SerdeConfig[] value();
  }
}
