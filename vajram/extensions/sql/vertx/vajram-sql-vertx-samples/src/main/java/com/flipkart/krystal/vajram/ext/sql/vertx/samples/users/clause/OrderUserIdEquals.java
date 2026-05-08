package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.WHERE;
import com.flipkart.krystal.vajram.ext.sql.statement.WhereClause;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderUserIdEquals extends WhereClause {
  long userId();
}
