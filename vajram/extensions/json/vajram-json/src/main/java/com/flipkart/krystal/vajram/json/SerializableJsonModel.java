package com.flipkart.krystal.vajram.json;

import static com.flipkart.krystal.vajram.json.Json.JSON;
import static com.flipkart.krystal.vajram.json.Json.OBJECT_WRITER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerializableModel;

@SupportedModelProtocol(Json.class)
public interface SerializableJsonModel extends SerializableModel {

  @Override
  default Json _serdeProtocol() {
    return JSON;
  }

  @Override
  default byte[] _serialize() throws JsonProcessingException {
    return OBJECT_WRITER.writeValueAsBytes(this);
  }

  default String _serializeAsString() throws JsonProcessingException {
    return OBJECT_WRITER.writeValueAsString(this);
  }
}
