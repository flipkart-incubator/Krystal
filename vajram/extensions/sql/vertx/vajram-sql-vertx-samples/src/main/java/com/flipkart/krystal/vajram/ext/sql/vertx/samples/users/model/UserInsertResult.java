package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.ReturnOnInsert;

/**
 * User-defined return-on-insert model for the {@link User} table. Declares that only the {@code id}
 * column should be returned by a SQL {@code RETURNING} clause after an INSERT operation.
 */
@ModelRoot(type = ModelRoot.ModelType.RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@ReturnOnInsert(value = true, inTable = User.class)
public interface UserInsertResult extends Model {
  long id();
}
