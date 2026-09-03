package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlSchema;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side operation root for {@code
 * VajramGraphQlTest#graphqlQueryWithQueryLevelAliases_succeeds}.
 */
@GraphQlSchema(path = "src/main/graphqls/Schema.graphqls")
@GraphQlOpRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetOrdersAliasedOp extends Model {

  @Field(name = "order")
  @FieldArg(name = "id", useVariable = "o1Id")
  OrderAliasedWithStateAlias o1();

  @Field(name = "order")
  @FieldArg(name = "id", useVariable = "o2Id")
  OrderAliased o2();
}
