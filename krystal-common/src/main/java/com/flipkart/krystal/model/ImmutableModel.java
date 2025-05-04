package com.flipkart.krystal.model;

public interface ImmutableModel extends Model {

  @Override
  default ImmutableModel _build() {
    return this;
  }

  interface Builder extends Model {

    @Override
    default Builder _asBuilder() {
      return this;
    }
  }
}
