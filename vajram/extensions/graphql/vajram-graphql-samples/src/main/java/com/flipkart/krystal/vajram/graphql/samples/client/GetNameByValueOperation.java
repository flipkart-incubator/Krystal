package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.FieldArg;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlOpRequest;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlSchema;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side operation root for {@code
 * VajramGraphQlTest#inferIdFromArgs_withOptionalIdField_buildsIdentityFromArgs}.
 */
@GraphQlSchema(path = "src/main/graphqls/Schema.graphqls")
@GraphQlOpRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface GetNameByValueOperation extends Model {

  @FieldArg(name = "value", useVariable = "value")
  @FieldArg(name = "string", useVariable = "string")
  NameFields name();
}
