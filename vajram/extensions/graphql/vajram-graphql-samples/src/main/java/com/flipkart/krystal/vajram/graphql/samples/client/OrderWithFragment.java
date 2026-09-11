package com.flipkart.krystal.vajram.graphql.samples.client;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlFragment;
import com.flipkart.krystal.vajram.graphql.client.api.GraphQlRequest;
import com.flipkart.krystal.vajram.json.Json;

/** Spreads {@link OrderFieldsFragment} - no fields of its own. */
@GraphQlRequest
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(Json.class)
public interface OrderWithFragment extends Model, @GraphQlFragment OrderFieldsFragment {}
