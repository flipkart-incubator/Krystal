package com.flipkart.krystal.model;

public interface ModelBuilder extends Model {

  @Override
  default ModelBuilder _asBuilder() {
    return this;
  }
}
