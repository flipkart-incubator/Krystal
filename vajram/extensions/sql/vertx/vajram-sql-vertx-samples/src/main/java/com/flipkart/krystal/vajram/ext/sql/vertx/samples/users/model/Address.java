package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.json.Json;

/**
 * A nested model representing a street address. Annotated with {@code @SupportedModelProtocol} so
 * that it can be serialized to JSON when persisted as a column in an SQL table.
 */
@ModelRoot
@SupportedModelProtocol(Json.class)
public interface Address extends Model {
  String city();

  String zip();
}
