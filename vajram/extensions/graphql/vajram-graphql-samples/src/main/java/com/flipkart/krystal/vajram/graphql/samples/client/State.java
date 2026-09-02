package com.flipkart.krystal.vajram.graphql.samples.client;

import com.flipkart.krystal.model.DefaultValue;
import com.flipkart.krystal.model.EnumModel;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.json.Json;

/**
 * Client-side mirror of the schema's {@code enum State}. Deliberately independent of the
 * server-generated {@code com.flipkart.krystal.vajram.graphql.samples.state.State} - a real
 * external GraphQL client wouldn't have access to that generated class, and referencing it from
 * these hand-written models creates a cross-annotation-processor forward reference that isn't
 * reliably resolvable within a single round.
 */
@ModelRoot
@SupportedModelProtocol(Json.class)
public enum State implements EnumModel {
  @DefaultValue
  UNKNOWN,
  PENDING,
  COMPLETED,
}
