package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.Projection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.OrderItem;

/** SELECT projection for individual order-item rows inside a nested LEFT JOIN. */
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = OrderItem.class)
public interface OrderItemInfo extends Model {

  long orderItemId();

  String itemName();

  long itemPriceCents();
}
