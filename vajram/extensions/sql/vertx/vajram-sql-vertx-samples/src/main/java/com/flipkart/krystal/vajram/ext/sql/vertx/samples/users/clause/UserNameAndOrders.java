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

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserNameAndOrders extends Model {
  String name();

  @ORDER_BY(column = "orderTime", direction = DESC)
  @LIMIT(10)
  List<OrderInfo> orders();
}
