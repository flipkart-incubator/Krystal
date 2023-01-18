package com.flipkart.krystal.vajram.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
  String TAG_KEY = "service";

  String value();
}
