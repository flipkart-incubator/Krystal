package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Client-side selection for {@code VajramGraphQlTest#graphqlQueryExecution_succeeds} - covers a
 * broad mix of scalar/enum fields, a duplicate-field alias, and the {@code __typename}
 * introspection meta-field.
 */
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderFullDetails extends Model {
  List<String> orderItemNames();

  String nameString();

  State state();

  State stateDuplicate();

  OffsetDateTime orderPlacedAt();

  Long orderItemsCount();

  LocalDate orderAcceptDate();

  @Field(name = "__typename")
  String typename();
}
