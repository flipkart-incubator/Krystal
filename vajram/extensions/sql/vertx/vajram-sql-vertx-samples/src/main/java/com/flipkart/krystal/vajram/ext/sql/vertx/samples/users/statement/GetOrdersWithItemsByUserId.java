package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ext.sql.statement.ORDER.Direction.DESC;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.ext.sql.statement.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.statement.ORDER;
import com.flipkart.krystal.vajram.ext.sql.statement.SELECT;
import com.flipkart.krystal.vajram.ext.sql.statement.SQL;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderUserIdEquals;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderWithItems;
import java.util.List;

/**
 * Fetches the 10 most recent orders (with their line items) for a user, sorted by {@code orderTime}
 * descending. Uses a LEFT JOIN from {@code orders} to {@code orderItems}.
 */
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrdersWithItemsByUserId
    extends TraitDef<@ORDER(by = "orderTime", direction = DESC) @LIMIT(10) List<OrderWithItems>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    OrderUserIdEquals where();
  }
}
