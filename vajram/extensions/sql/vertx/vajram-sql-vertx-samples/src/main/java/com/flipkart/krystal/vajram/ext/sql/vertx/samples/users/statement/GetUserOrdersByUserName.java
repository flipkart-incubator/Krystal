package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.ext.sql.statement.SELECT;
import com.flipkart.krystal.vajram.ext.sql.statement.SQL;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameAndOrders;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserNameEquals;

@SQL
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserOrdersByUserName extends TraitDef<UserNameAndOrders> {
  interface _Inputs {
    @IfAbsent(FAIL)
    UserNameEquals where();
  }
}
