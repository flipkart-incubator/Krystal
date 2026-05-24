package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsGreaterThan;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

/** WHERE predicate: {@code amountCents > $1}. Demonstrates {@code @IsGreaterThan}. */
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderAmountGtPredicate extends SelectionPredicate {

  @Column("amountCents")
  @IsGreaterThan
  long amountGreaterThan();

  static OrderAmountGtPredicate_ImmutPojo.Builder _builder() {
    return OrderAmountGtPredicate_ImmutPojo._builder();
  }
}
