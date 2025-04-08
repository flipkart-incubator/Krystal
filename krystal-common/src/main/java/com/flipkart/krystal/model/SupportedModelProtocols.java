package com.flipkart.krystal.model;

public @interface SupportedModelProtocols {
  Class<? extends ModelProtocol>[] value();
}
