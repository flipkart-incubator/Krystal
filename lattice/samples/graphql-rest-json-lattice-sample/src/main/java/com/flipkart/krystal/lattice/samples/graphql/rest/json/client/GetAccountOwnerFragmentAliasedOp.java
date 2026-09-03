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
 * GraphQlEndpointsE2eTest#graphQlQuery_namedFragment_withAliasesAtSpreadSiteAndInsideFragment_succeeds}
 * - exercises a named GraphQL fragment spread with {@code @Field(name=...)} aliasing both at the
 * spread site ({@code accountAlias}/{@code ownerAlias}) and inside the fragment itself.
 */
@GraphQlOpRequest(schemaFilePath = "src/main/graphqls/Schema.graphqls")
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetAccountOwnerFragmentAliasedOp extends Model {

  @Field(name = "account")
  @FieldArg(name = "id", useVariable = "id")
  AccountAliasedWithOwnerFragment accountAlias();
}
