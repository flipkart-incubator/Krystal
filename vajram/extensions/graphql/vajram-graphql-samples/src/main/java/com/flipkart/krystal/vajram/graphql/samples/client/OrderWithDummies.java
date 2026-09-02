package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;

@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderWithDummies extends Model {
  @Field(name = "dummy")
  @FieldArg(name = "name", useVariable = "d1Name")
  DummyIdOnly d1();

  @Field(name = "dummy")
  @FieldArg(name = "name", useVariable = "d2Name")
  DummyIdOnly d2();
}
