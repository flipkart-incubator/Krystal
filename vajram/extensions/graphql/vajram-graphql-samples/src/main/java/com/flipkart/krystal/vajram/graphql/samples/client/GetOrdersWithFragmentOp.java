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
 * Client-side operation root for {@code
 * VajramGraphQlTest#graphqlQueryWithNamedFragment_underQueryLevelAliases_succeeds}. The same named
 * fragment ({@link OrderFieldsFragment}, spread via {@link OrderWithFragment}) is used under two
 * differently-aliased {@code order} selections.
 */
@GraphQlOpRequest(schemaFilePath = "src/main/graphqls/Schema.graphqls")
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetOrdersWithFragmentOp extends Model {

  @Field(name = "order")
  @FieldArg(name = "id", useVariable = "o1Id")
  OrderWithFragment o1();

  @Field(name = "order")
  @FieldArg(name = "id", useVariable = "o2Id")
  OrderWithFragment o2();
}
