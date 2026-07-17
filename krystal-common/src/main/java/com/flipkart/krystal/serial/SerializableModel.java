package com.flipkart.krystal.serial;

import java.io.InputStream;

public interface SerializableModel {

  /**
   * Returns a non-blocking {@link InputStream} representing the serialized version of this object.
   */
  InputStream _serialize() throws Exception;

  SerdeProtocol _serdeProtocol();
}
