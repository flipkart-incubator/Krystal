package com.flipkart.krystal.model;

public interface ModelProtocol {
  String modelClassesSuffix();

  default boolean isPurityMandatory() {
    return false;
  }
}
