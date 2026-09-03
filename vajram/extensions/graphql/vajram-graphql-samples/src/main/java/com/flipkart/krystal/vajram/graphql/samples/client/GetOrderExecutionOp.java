package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side operation root for {@code VajramGraphQlTest#graphqlQueryExecution_succeeds}. Covers
 * multiple root fields, nested scalar/enum fields, and {@code __typename} at both the root and
 * nested levels.
 */
@GraphQlOpRequest(schemaFilePath = "src/main/graphqls/Schema.graphqls")
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetOrderExecutionOp extends Model {

  @FieldArg(name = "id", useVariable = "orderId")
  OrderFullDetails order();

  @FieldArg(name = "dummyId", useVariable = "dummyId")
  DummyFullDetails dummy();

  @FieldArg(name = "userId", useVariable = "userId")
  OrderItemNamesOnly mostRecentOrder();

  @Field(name = "__typename")
  String typename();
}
