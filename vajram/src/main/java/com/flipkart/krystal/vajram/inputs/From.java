package com.flipkart.krystal.vajram.inputs;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface From {
  String value();
}
