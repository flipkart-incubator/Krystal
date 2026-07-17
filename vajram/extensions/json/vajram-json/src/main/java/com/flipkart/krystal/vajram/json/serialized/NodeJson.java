package com.flipkart.krystal.vajram.json.serialized;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.flipkart.krystal.vajram.json.Json;
import java.io.IOException;

public final class NodeJson extends AbstractJsonRepresentation {

  private final JsonNode jsonNode;

  /* ----Derived fields ----- */
  private byte[] bytes;

  public NodeJson(JsonNode jsonNode) {
    this.jsonNode = jsonNode;
  }

  @Override
  public <T> T deserialize(ObjectReader reader) throws IOException {
    return reader.readValue(jsonNode);
  }

  @Override
  protected byte[] asBytes() {
    if (bytes == null) {
      try {
        bytes = Json.OBJECT_WRITER.writeValueAsBytes(jsonNode);
      } catch (JsonProcessingException e) {
        throw wrapAsCompletionException(e, "Unable to convert json node to byte array");
      }
    }
    return bytes;
  }
}
