package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.INSERT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.User;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.model.UserInsertResult;

/**
 * Inserts a single user and returns the generated {@code id} via the SQL {@code RETURNING} clause.
 * The response type is the user-defined {@link UserInsertResult} interface annotated with
 * {@code @ReturnOnInsert}, declaring which columns should be returned from the {@link User} table.
 */
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@INSERT
@Trait
@CallGraphDelegationMode(SYNC)
public interface InsertUserReturning extends TraitDef<UserInsertResult> {
  interface _Inputs {
    @IfAbsent(FAIL)
    User user();
  }
}
