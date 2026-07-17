package com.flipkart.krystal.serial;

import java.io.IOException;
import java.io.OutputStream;

public interface SerializableModel {
  byte[] _serialize() throws Exception;

  void _serialize(OutputStream outputStream) throws IOException;

  SerdeProtocol _serdeProtocol();
}
