package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.model.PlainJavaObject;
import com.flipkart.krystal.model.SupportedModelProtocol;
import com.flipkart.krystal.vajram.ext.sql.lang.ColumnPredicate;
import com.flipkart.krystal.vajram.ext.sql.lang.WHERE;
import com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison.IsInRange;
import com.flipkart.krystal.vajram.ext.sql.model.Column;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.Order;
import com.google.common.collect.Range;

/**
 * WHERE predicate using {@code @IsInRange} to filter orders whose {@code orderTime} falls within a
 * client-provided {@link Range}. The range can be open, closed, or half-open/half-closed — the
 * generated SQL adapts the comparison operators accordingly at runtime.
 */
@ModelRoot
@SupportedModelProtocol(PlainJavaObject.class)
@WHERE(inTable = Order.class)
public interface OrderTimeIsInRange extends ColumnPredicate {

  @Column("orderTime")
  @IsInRange
  Range<Long> orderTimeRange();

  static OrderTimeIsInRange_ImmutPojo.Builder _builder() {
    return OrderTimeIsInRange_ImmutPojo._builder();
  }
}
