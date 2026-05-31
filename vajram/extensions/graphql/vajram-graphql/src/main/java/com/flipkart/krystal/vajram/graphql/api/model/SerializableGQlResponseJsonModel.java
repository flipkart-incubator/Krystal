package com.flipkart.krystal.vajram.graphql.api.model;

import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.serial.SerializableModel;
import com.flipkart.krystal.vajram.json.Json;

@SupportedModelProtocol(Json.class)
public interface SerializableGQlResponseJsonModel extends SerializableModel {

  @Override
  default GraphQlResponseJson _serdeProtocol() {
    return GraphQlResponseJson.INSTANCE;
  }
}
