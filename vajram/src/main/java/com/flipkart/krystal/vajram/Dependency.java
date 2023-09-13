package com.flipkart.krystal.vajram;

import static com.flipkart.krystal.vajram.Dependency.DependencyType.VAJRAM;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Dependency {

  String value();

  boolean canFanout() default false;

  DependencyType type() default VAJRAM;

  enum DependencyType {
    VAJRAM
  }
}
