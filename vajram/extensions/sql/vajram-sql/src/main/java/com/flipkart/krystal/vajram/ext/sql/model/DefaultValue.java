package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/** Specifies the default value to be stored in a column */
@Target(METHOD)
public @interface DefaultValue {
  String value();
}
