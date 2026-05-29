package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Repeatable(SupportedModelProtocol.SupportedModelProtocols.class)
@Target({TYPE, TYPE_USE})
public @interface SupportedModelProtocol {
  Class<? extends ModelProtocol> value();

  @Target(TYPE)
  @interface SupportedModelProtocols {
    SupportedModelProtocol[] value();
  }
}
