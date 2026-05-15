package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model;

import com.flipkart.krystal.model.ModelRoot;
import com.flipkart.krystal.vajram.ext.sql.model.ForeignKey;
import com.flipkart.krystal.vajram.ext.sql.model.PrimaryKey;
import com.flipkart.krystal.vajram.ext.sql.model.Table;
import com.flipkart.krystal.vajram.ext.sql.model.TableModel;

@ModelRoot
@Table(name = "orderItems")
public interface OrderItem extends TableModel {
  @PrimaryKey
  long orderItemId();

  String itemName();

  long itemPriceCents();

  @ForeignKey
  Order orderId();
}
