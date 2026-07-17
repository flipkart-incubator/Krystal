package com.flipkart.krystal.vajram.json;

import static com.flipkart.krystal.vajram.json.Json.JSON;
import static com.flipkart.krystal.vajram.json.Json.OBJECT_READER;
import static com.flipkart.krystal.vajram.json.Json.OBJECT_WRITER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.serialized.BytesJson;
import com.flipkart.krystal.vajram.json.serialized.JsonRepresentation;
import java.io.InputStream;

@SupportedModelProtocol(Json.class)
public interface SerializableJsonModel extends SerializableModel {

  @Override
  default Json _serdeProtocol() {
    return JSON;
  }

  @Override
  default InputStream _serialize() throws JsonProcessingException {
    return _serializedJson().newInputStream();
  }

  default JsonRepresentation _serializedJson() throws JsonProcessingException {
    return new BytesJson(_writer().writeValueAsBytes(this));
  }

  default ObjectReader _reader() {
    return OBJECT_READER;
  }

  default ObjectWriter _writer() {
    return OBJECT_WRITER;
  }
}
