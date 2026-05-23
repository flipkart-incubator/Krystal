package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.LesserThanOrEqual;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

/** WHERE predicate: {@code amountCents <= $1}. Tests {@code @LesserThanOrEqual}. */
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderAmountLtePredicate extends SelectionPredicate {

  @Column("amountCents")
  @LesserThanOrEqual
  long amountAtMost();

  static OrderAmountLtePredicate_ImmutPojo.Builder _builder() {
    return OrderAmountLtePredicate_ImmutPojo._builder();
  }
}
