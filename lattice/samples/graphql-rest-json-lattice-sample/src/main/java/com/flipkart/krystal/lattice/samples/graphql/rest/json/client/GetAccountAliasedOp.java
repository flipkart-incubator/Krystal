package com.flipkart.krystal.lattice.samples.graphql.rest.json.client;

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
 * GraphQlEndpointsE2eTest#graphQlQuery_aliasesOperationNormalAndComposedOnlyFields} - exercises
 * {@code @Field(name=...)} aliasing at every nesting level, including the {@code @composedOnly}
 * {@code imageData} type.
 */
@GraphQlOpRequest(schemaFilePath = "src/main/graphqls/Schema.graphqls")
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetAccountAliasedOp extends Model {

  @Field(name = "account")
  @FieldArg(name = "id", useVariable = "id")
  AccountAliased accountAlias();
}
