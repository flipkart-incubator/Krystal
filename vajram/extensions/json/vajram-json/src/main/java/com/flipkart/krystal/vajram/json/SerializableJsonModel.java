package com.flipkart.krystal.vajram.json;

import static com.flipkart.krystal.vajram.json.Json.JSON;

import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;

@SupportedModelProtocols(Json.class)
public interface SerializableJsonModel extends SerializableModel {

  @Override
  default SerdeProtocol _serdeProtocol() {
    return JSON;
  }

  @Override
  default byte[] _serialize() throws Exception {
    return new byte[0];
  }
}
