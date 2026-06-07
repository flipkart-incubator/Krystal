package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.SELECT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserIdPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithOrdersAndItems;

/**
 * Fetches a user together with all their orders and each order's line items via a two-level nested
 * LEFT JOIN (users → orders → orderItems).
 */
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserByIdWithOrdersAndItems extends TraitDef<UserWithOrdersAndItems> {
  interface _Inputs {
    @IfAbsent(FAIL)
    UserIdPredicate where();
  }
}
