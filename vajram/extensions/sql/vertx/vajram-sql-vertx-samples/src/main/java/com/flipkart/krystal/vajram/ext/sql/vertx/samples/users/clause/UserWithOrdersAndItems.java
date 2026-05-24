package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.vajram.ext.sql.lang.ORDER.Direction.DESC;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.lang.ORDER;
import com.flipkart.krystal.vajram.ext.sql.model.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.List;

@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@Selection(from = User.class)
public interface UserWithOrdersAndItems extends Model {

  String name();

  @ORDER(by = "orderTime", direction = DESC)
  @LIMIT(3)
  List<OrderWithItems> orders();
}
