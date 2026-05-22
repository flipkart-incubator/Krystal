package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Repeatable(SupportedModelProtocol.SupportedModelProtocols.class)
@Target(TYPE)
@Retention(CLASS)
public @interface SupportedModelProtocol {
  Class<? extends ModelProtocol> value();

  @Target(TYPE)
  @Retention(CLASS)
  @interface SupportedModelProtocols {
    SupportedModelProtocol[] value();
  }
}
