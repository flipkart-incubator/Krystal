package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ext.sql.lang.ORDER.Direction.ASC;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.lang.ORDER;
import com.flipkart.krystal.vajram.ext.sql.lang.SELECT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderInfo;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderTimeRangePredicate;
import java.util.List;

/**
 * Fetches orders within a half-open time range {@code [from, to)}, sorted ascending. Tests
 * {@code @IsGreaterThanOrEqual} and {@code @IsLessThan} operators.
 */
@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrdersByTimeRange
    extends TraitDef<
        @ORDER(by = "orderTime", direction = ASC) @LIMIT(LIMIT.NO_LIMIT) List<OrderInfo>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    OrderTimeRangePredicate where();
  }
}
