package com.flipkart.krystal.serial;

import com.flipkart.krystal.model.ModelProtocol;

/** Represents a serialization protocol used to serialize and deserialize facets values. */
public interface SerdeProtocol extends ModelProtocol {

  /**
   * Returns the content-type which represents this serde protocol. This generally used as values of
   * "Accept" request header and "Content-Type" response header in client-server communication
   * protocols.
   */
  String defaultContentType();
}
