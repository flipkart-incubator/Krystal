package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.lang.SELECT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderAmountLtePredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.OrderInfo;
import java.util.List;

/** Fetches orders with {@code amountCents <= threshold}. Tests {@code @IsLessThanOrEqual}. */
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetOrdersByMaxAmount extends TraitDef<@LIMIT(LIMIT.NO_LIMIT) List<OrderInfo>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    OrderAmountLtePredicate where();
  }
}
