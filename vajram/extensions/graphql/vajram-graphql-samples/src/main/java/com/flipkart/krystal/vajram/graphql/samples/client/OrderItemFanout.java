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
public interface OrderItemFanout extends Model {
  @Field(name = "orderItemAt")
  @FieldArg(name = "index", useVariable = "i1Index")
  String i1();

  @Field(name = "orderItemAt")
  @FieldArg(name = "index", useVariable = "i2Index")
  String i2();

  @Field(name = "orderItemNamesFrom")
  @FieldArg(name = "offset", useVariable = "f1Offset")
  List<String> f1();

  @Field(name = "orderItemNamesFrom")
  @FieldArg(name = "offset", useVariable = "f2Offset")
  List<String> f2();
}
