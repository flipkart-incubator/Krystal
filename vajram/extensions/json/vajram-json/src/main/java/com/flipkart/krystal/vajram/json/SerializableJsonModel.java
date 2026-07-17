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
import com.flipkart.krystal.vajram.json.serialized.SerializedJson;
import java.io.IOException;
import java.io.OutputStream;

@SupportedModelProtocol(Json.class)
public interface SerializableJsonModel extends SerializableModel {

  @Override
  default Json _serdeProtocol() {
    return JSON;
  }

  @Override
  default byte[] _serialize() throws JsonProcessingException {
    return _serializedJson().asBytes().clone();
  }

  @Override
  default void _serialize(OutputStream outputStream) throws IOException {
    _writer().writeValue(outputStream, this);
  }

  default SerializedJson _serializedJson() throws JsonProcessingException {
    return new BytesJson(_writer().writeValueAsBytes(this));
  }

  default ObjectReader _reader() {
    return OBJECT_READER;
  }

  default ObjectWriter _writer() {
    return OBJECT_WRITER;
  }
}
