package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.ColumnPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsEqualTo;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;

@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = User.class)
public interface UserNamePredicate extends ColumnPredicate {
  @Column("name")
  @IsEqualTo
  String nameIs();

  static UserNamePredicate_Immut.Builder _builder() {
    return UserNamePredicate_ImmutPojo._builder();
  }
}
