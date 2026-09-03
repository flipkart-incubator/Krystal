package com.flipkart.krystal.lattice.samples.graphql.rest.json.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;

/** {@code fragment personFields on Person { name { firstName lastName } email }} */
@GraphQlFragment
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface PersonFields extends Model {
  OwnerNameDetails name();

  String email();
}
