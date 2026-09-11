package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;
import java.util.List;

/** A reusable named fragment on {@code Order}, spread via {@link OrderWithFragment}. */
@GraphQlFragment
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderFieldsFragment extends Model {
  State state();

  List<String> orderItemNames();
}
