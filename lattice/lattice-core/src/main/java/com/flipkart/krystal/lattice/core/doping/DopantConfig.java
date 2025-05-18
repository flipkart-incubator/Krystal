package com.flipkart.krystal.lattice.core.dopants;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type", include = WRAPPER_OBJECT)
public interface DopantConfig {

  String _dopantType();

  record NoConfiguration() implements DopantConfig {

    @Override
    public String _dopantType() {
      return "lattice.NoConfiguration";
    }
  }

  @interface NoAnnotation {}
}
