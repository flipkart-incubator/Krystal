package com.flipkart.krystal.vajram.ext.sql.model;

import static java.lang.annotation.ElementType.TYPE_USE;

import com.flipkart.krystal.serial.SerdeProtocol;
import java.lang.annotation.Target;

@Target(TYPE_USE)
public @interface SerdeWith {
  Class<? extends SerdeProtocol> value();
}
