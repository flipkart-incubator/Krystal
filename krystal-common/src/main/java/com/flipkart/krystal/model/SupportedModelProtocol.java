package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.model.SupportedModelProtocol.SupportedModelProtocols;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Target;

@Repeatable(SupportedModelProtocols.class)
@Target(TYPE)
public @interface SupportedModelProtocol {
  Class<? extends ModelProtocol> value();

  @Target(TYPE)
  @interface SupportedModelProtocols {
    SupportedModelProtocol[] value();
  }
}
