package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.INSERT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;

/** Inserts a single user into the {@code users} table. Returns the number of rows inserted. */
@SQL
@INSERT
@Trait
@CallGraphDelegationMode(SYNC)
public interface InsertUser extends TraitDef<Integer> {
  interface _Inputs {
    @IfAbsent(FAIL)
    User user();
  }
}
