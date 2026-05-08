package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.vajram.ext.sql.statement.ORDER_BY.Direction.DESC;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER_BY;
import com.flipkart.krystal.vajram.ext.sql.statement.Projection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.List;

/**
 * Two-level nested LEFT JOIN projection: a user → their orders → each order's line items.
 *
 * <p>The codegen produces a single SQL query:
 *
 * <pre>
 * SELECT users.id AS users_id, users.name AS users_name,
 *        orders.orderId AS orders_orderId, orders.amountCents AS orders_amountCents,
 *        orders.orderTime AS orders_orderTime,
 *        orderItems.orderItemId AS orderItems_orderItemId,
 *        orderItems.itemName AS orderItems_itemName,
 *        orderItems.itemPriceCents AS orderItems_itemPriceCents
 * FROM users
 * LEFT JOIN orders ON users.id = orders.userId
 * LEFT JOIN orderItems ON orders.orderId = orderItems.order
 * WHERE users.id = $1
 * </pre>
 */
@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserWithOrdersAndItems extends Model {

  String name();

  @ORDER_BY(column = "orderTime", direction = DESC)
  @LIMIT(3)
  List<OrderWithItems> orders();
}
