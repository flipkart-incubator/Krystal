package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;

@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderAliasedWithStateAlias extends Model {
  @Field(name = "state")
  State s();

  List<String> orderItemNames();
}
