package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side operation root for {@code
 * VajramGraphQlTest#argLessListIdFetcherAliases_whenIdFetcherFails_surfacesSameErrorForAllAliases}.
 */
@GraphQlOpRequest(schemaFilePath = "src/main/graphqls/Schema.graphqls")
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetOrderNoArgDummiesFanoutOp extends Model {

  @FieldArg(name = "id", useVariable = "id")
  OrderNoArgDummiesFanout order();
}
