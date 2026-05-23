package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.logical.SqlOrPredicate;

@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
public interface UserOrPredicate extends SqlOrPredicate {
  UserIdPredicate orWithUserId();

  UserNamePredicate orWithUserName();

  static UserOrPredicate_Immut.Builder _builder() {
    return UserOrPredicate_ImmutPojo._builder();
  }
}
