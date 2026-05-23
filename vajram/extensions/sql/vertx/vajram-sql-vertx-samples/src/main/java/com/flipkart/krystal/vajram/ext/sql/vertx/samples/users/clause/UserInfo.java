package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.model.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.Optional;

@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@Selection(from = User.class)
public interface UserInfo extends Model {

  long id();

  String name();

  /** DB column is {@code email}; this method uses an alias. */
  @Column("email")
  String contactEmail();

  Optional<String> phoneNumber();
}
