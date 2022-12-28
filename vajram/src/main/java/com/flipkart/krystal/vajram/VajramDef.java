package com.flipkart.krystal.vajram;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface VajramDef {
  String value();
}
