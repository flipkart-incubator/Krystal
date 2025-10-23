package com.flipkart.krystal.vajram.samples.sql;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.sql.SqlQuery;
import java.util.List;

/** SQL Trait for fetching users by ID. */
@Trait
@CallGraphDelegationMode(SYNC)
@SqlQuery("SELECT id, name, email_id FROM user_profile WHERE id = ?")
public interface GetUserByIdTrait extends TraitRoot<List<User>> {

  // Input parameters for the SQL trait.
  class _Inputs {
    /**
     * Query parameters (e.g., user ID). For the query "SELECT * FROM users WHERE id = ?", this
     * would be List.of(userId).
     */
    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
