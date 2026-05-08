package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.ext.sql.model.ForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;

/**
 * Sample table model for the {@code orders} table.
 *
 * <p>Demonstrates {@code @ForeignKey} referencing {@link User}.
 */
@ModelRoot
@Table(name = "orders")
public interface Order extends TableModel {

  @PrimaryKey
  long orderId();

  /** FK to {@code users.id}. The field type matches the referenced table's model type. */
  @ForeignKey
  User userId();

  long amountCents();

  long orderTime();
}
