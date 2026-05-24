package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.SelectionPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsGreaterThanOrEqual;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsLessThan;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;

/**
 * WHERE predicate for a half-open time range: {@code orderTime >= $1 AND orderTime < $2}. Tests
 * both {@code @IsGreaterThanOrEqual} and {@code @IsLessThan}.
 */
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderTimeRangePredicate extends SelectionPredicate {

  @Column("orderTime")
  @IsGreaterThanOrEqual
  long orderTimeFrom();

  @Column("orderTime")
  @IsLessThan
  long orderTimeTo();

  static OrderTimeRangePredicate_ImmutPojo.Builder _builder() {
    return OrderTimeRangePredicate_ImmutPojo._builder();
  }
}
