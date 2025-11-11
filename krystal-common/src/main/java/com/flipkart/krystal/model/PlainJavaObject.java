package com.flipkart.krystal.model;

public final class PlainJavaObject implements ModelProtocol {
  public static final PlainJavaObject POJO = new PlainJavaObject();

  @Override
  public String modelClassesSuffix() {
    return "Pojo";
  }

  private PlainJavaObject() {}
}
