package com.flipkart.krystal.vajram.samples.sql;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.sql.SqlQuery;
import com.flipkart.krystal.vajram.sql.SqlUpdate;
import java.util.List;

@Trait
@CallGraphDelegationMode(SYNC)
@SqlUpdate("INSERT INTO user_profile(name, email_id) values(?, ?)")
public interface CreateUserTrait extends TraitRoot<List<User>> {

  class _Inputs {

    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
