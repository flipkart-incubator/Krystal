package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.Field;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;

/**
 * Same fields as {@link OrderFieldsFragment}, but aliased *inside* the fragment definition itself -
 * spread via {@link OrderWithAliasedFragment}.
 */
@GraphQlFragment
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderFieldsAliasedFragment extends Model {
  @Field(name = "state")
  State s();

  @Field(name = "orderItemNames")
  List<String> names();
}
