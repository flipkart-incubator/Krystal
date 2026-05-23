package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsEqualTo;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;

@ModelRoot
@WHERE(inTable = User.class)
public interface UserIdPredicate extends SelectionPredicate {
  @Column("id")
  @IsEqualTo
  long idIs();

  static UserIdPredicate_Immut.Builder _builder() {
    return UserIdPredicate_ImmutPojo._builder();
  }
}
