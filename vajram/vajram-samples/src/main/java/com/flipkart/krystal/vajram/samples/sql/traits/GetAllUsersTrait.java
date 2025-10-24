package com.flipkart.krystal.vajram.samples.sql.traits;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.samples.sql.User;
import com.flipkart.krystal.vajram.sql.annotations.SqlQuery;
import java.util.List;

/** SQL Trait for fetching all users. A simple query without parameters. */
@Trait
@CallGraphDelegationMode(SYNC)
@SqlQuery("SELECT id, name, email_id FROM user_profile")
public interface GetAllUsersTrait extends TraitRoot<List<User>> {

  // Input parameters for the SQL trait.
  class _Inputs {

    // Query parameters (empty for this query).
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
