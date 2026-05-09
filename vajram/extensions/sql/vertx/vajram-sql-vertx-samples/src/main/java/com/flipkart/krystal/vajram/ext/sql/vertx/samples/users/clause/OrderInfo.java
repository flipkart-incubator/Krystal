package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

/** SELECT query shape that fetches some order info */
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Selection(from = Order.class)
public interface OrderInfo extends Model {

  long orderId();

  long userId();

  long amountCents();
}
