package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.ColumnPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsLessThanOrEqual;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

/** WHERE predicate: {@code amountCents <= $1}. Demonstrates {@code @IsLessThanOrEqual}. */
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderAmountLtePredicate extends ColumnPredicate {

  @Column("amountCents")
  @IsLessThanOrEqual
  long amountAtMost();

  static OrderAmountLtePredicate_ImmutPojo.Builder _builder() {
    return OrderAmountLtePredicate_ImmutPojo._builder();
  }
}
