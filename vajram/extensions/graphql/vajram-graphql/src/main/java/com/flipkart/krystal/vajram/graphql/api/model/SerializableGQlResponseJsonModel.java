package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.serial.SerdeProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.Json;

@SupportedModelProtocols(Json.class)
public interface SerializableGQlResponseJsonModel extends SerializableModel {

  @Override
  default SerdeProtocol _serdeProtocol() {
    return GraphQlResponseJson.INSTANCE;
  }
}
