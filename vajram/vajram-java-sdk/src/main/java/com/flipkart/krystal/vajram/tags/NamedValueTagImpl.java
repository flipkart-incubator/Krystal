package com.flipkart.krystal.vajram.tags;

import java.lang.annotation.Annotation;

record NamedValueTagImpl(String name, String value) implements NamedValueTag {

  @Override
  public Class<? extends Annotation> annotationType() {
    return NamedValueTag.class;
  }
}
