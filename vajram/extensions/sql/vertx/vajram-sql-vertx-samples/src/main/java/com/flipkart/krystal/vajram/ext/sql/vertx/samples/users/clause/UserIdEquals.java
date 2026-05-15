package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocols;
import com.flipkart.krystal.vajram.ext.sql.statement.WHERE;
import com.flipkart.krystal.vajram.ext.sql.statement.WhereClause;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;

@ModelRoot
@SupportedModelProtocols(PlainJavaObject.class)
@WHERE(inTable = User.class)
public interface UserIdEquals extends WhereClause {
  long id();
}
