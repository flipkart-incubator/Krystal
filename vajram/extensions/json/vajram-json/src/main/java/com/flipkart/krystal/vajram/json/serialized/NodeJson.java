package com.flipkart.krystal.vajram.json.serialized;

import com.flipkart.krystal.vajram.json.Json;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectReader;

public final class NodeJson extends AbstractJsonRepresentation {

  private final JsonNode jsonNode;

  /* ----Derived fields ----- */
  private byte @MonotonicNonNull [] bytes;

  public NodeJson(JsonNode jsonNode) {
    this.jsonNode = jsonNode;
  }

  @Override
  public <T> T _deserialize(ObjectReader reader) {
    return reader.readValue(jsonNode);
  }

  @Override
  protected byte[] asBytes() {
    if (bytes == null) {
      bytes = Json.OBJECT_WRITER.writeValueAsBytes(jsonNode);
    }
    return bytes;
  }
}
