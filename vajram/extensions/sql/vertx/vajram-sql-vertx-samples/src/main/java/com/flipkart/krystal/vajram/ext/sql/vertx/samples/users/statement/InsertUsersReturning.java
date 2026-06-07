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
import java.util.List;

/**
 * Inserts multiple users and returns the generated {@code id} for each via the SQL {@code
 * RETURNING} clause. The response type is {@code List<UserInsertResult>} — one entry per inserted
 * row, containing the columns declared in the {@link UserInsertResult} interface.
 */
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@INSERT
@Trait
@CallGraphDelegationMode(SYNC)
public interface InsertUsersReturning extends TraitDef<List<UserInsertResult>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    List<User> users();
  }
}
