package com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.statement;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;
import static com.flipkart.krystal.vajram.ext.sql.lang.SqlDialect.POSTGRESQL_18;

import com.flipkart.krystal.annos.CallGraphDelegationMode;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitDef;
import com.flipkart.krystal.vajram.ext.sql.lang.LIMIT;
import com.flipkart.krystal.vajram.ext.sql.lang.SELECT;
import com.flipkart.krystal.vajram.ext.sql.lang.SQL;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.EmbeddingIdPredicate;
import com.flipkart.krystal.vajram.ext.sql.vertx.samples.users.clause.EmbeddingInfo;

@SQL(dialect = POSTGRESQL_18)
@SELECT
@Trait
@CallGraphDelegationMode(SYNC)
public interface GetEmbeddingById extends TraitDef<@LIMIT(1) EmbeddingInfo> {
  interface _Inputs {
    @IfAbsent(FAIL)
    EmbeddingIdPredicate where();
  }
}
