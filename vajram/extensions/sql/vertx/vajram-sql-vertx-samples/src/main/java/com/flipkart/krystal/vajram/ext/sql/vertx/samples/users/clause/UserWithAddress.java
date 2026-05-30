package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import static com.flipkart.krystal.model.ModelRoot.ModelType.RESPONSE;

import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.model.Selection;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Address;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import java.util.List;

/**
 * Selection interface that includes columns annotated with {@code @SerdeWith} in the underlying
 * {@link User} table. Used to test SELECT deserialization of JSON-serialized columns.
 */
@ModelRoot(type = RESPONSE)
@SupportedModelProtocol(PlainJavaObject.class)
@Selection(from = User.class)
public interface UserWithAddress extends Model {

  long id();

  String name();

  Address address();

  List<Address> secondaryAddresses();
}
