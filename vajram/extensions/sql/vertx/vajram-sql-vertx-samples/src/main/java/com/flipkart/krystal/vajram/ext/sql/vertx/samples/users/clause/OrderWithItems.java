package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.vajram.ext.sql.statement.ORDER.Direction.DESC;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER;
import com.flipkart.krystal.vajram.ext.sql.statement.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;
import java.util.List;

/**
 * SELECT projection for an order together with its line items.
 *
 * <p>The {@code List<OrderItemInfo>} method triggers a nested LEFT JOIN: {@code orders LEFT JOIN
 * orderItems ON orders.orderId = orderItems.order}.
 */
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Selection(from = Order.class)
public interface OrderWithItems extends Model {

  long orderId();

  long amountCents();

  long orderTime();

  @ORDER(by = "itemPriceCents", direction = DESC)
  @LIMIT(5)
  List<OrderItemInfo> orderItems();
}
