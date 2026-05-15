package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.vajram.ext.sql.statement.ORDER.Direction.DESC;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER;
import com.flipkart.krystal.vajram.ext.sql.statement.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.List;

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Selection(from = User.class)
public interface UserNameAndOrders extends Model {
  String name();

  @ORDER(by = "orderTime", direction = DESC)
  @LIMIT(10)
  List<OrderInfo> orders();
}
