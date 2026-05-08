package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.Column;
import com.flipkart.krystal.vajram.ext.sql.statement.Projection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.Optional;

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@Projection(over = User.class)
public interface UserInfo extends Model {

  long id();

  String name();

  /** DB column is {@code email}; this method uses an alias. */
  @Column("email")
  String contactEmail();

  Optional<String> phoneNumber();
}
