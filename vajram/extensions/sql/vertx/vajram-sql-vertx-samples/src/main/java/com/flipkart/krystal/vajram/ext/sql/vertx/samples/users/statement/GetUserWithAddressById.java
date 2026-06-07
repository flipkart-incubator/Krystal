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
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserIdPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.UserWithAddress;

/**
 * SELECT trait that returns a user with their JSON-serialized address columns deserialized via
 * {@code SerdeProtocol.deserialize}.
 */
@SQL(dialect = SqlDialect.POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetUserWithAddressById extends TraitDef<@LIMIT(1) UserWithAddress> {
  interface _Inputs {
    @IfAbsent(FAIL)
    UserIdPredicate where();
  }
}
