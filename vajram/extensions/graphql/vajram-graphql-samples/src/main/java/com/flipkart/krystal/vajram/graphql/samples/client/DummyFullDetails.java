package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side selection for {@code VajramGraphQlTest#graphqlQueryExecution_succeeds}, including the
 * {@code __typename} introspection meta-field.
 */
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface DummyFullDetails extends Model {
  String name();

  Integer age();

  String f1();

  @Field(name = "__typename")
  String typename();
}
