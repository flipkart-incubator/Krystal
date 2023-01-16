package com.flipkart.krystal.vajram;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
public @interface Tag {
  String name();

  String value();
}
