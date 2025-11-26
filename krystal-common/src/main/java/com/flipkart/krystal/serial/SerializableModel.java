package com.flipkart.krystal.serial;

public interface SerializableModel {
  byte[] _serialize() throws Exception;

  SerdeProtocol _serdeProtocol();
}
