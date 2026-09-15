package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;

@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderDummiesFanout extends Model {
  @Field(name = "dummies")
  @FieldArg(name = "filter", useVariable = "okFilter")
  @FieldArg(name = "preferredType", useVariable = "okPreferredType")
  @FieldArg(name = "count", useVariable = "okCount")
  List<DummyIdOnly> ok();

  @Field(name = "dummies")
  @FieldArg(name = "filter", useVariable = "badFilter")
  @FieldArg(name = "preferredType", useVariable = "badPreferredType")
  @FieldArg(name = "count", useVariable = "badCount")
  List<DummyIdOnly> bad();
}
