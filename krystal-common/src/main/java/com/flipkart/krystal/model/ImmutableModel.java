package com.flipkart.krystal.model;

public interface ImmutableModel extends Model {

  @Override
  default ImmutableModel _build() {
    return this;
  }
}
