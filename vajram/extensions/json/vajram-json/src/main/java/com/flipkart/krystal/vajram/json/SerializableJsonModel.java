package com.flipkart.krystal.vajram.json;

import static com.flipkart.krystal.vajram.json.Json.JSON;
import static com.flipkart.krystal.vajram.json.Json.OBJECT_READER;
import static com.flipkart.krystal.vajram.json.Json.OBJECT_WRITER;

import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.serialized.BytesJson;
import com.flipkart.krystal.vajram.json.serialized.JsonRepresentation;
import java.io.InputStream;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;

@SupportedModelProtocol(Json.class)
public interface SerializableJsonModel extends SerializableModel {

  @Override
  default Json _serdeProtocol() {
    return JSON;
  }

  @Override
  default InputStream _serialize() {
    return _serializedJson()._serialize();
  }

  default JsonRepresentation _serializedJson() {
    return new BytesJson(_writer().writeValueAsBytes(this));
  }

  default ObjectReader _reader() {
    return OBJECT_READER;
  }

  default ObjectWriter _writer() {
    return OBJECT_WRITER;
  }
}
