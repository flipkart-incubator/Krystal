package com.flipkart.krystal.vajram.samples.sql.traits;

import static com.flipkart.krystal.annos.ComputeDelegationMode.SYNC;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.Trait;
import com.flipkart.krystal.vajram.TraitRoot;
import com.flipkart.krystal.vajram.annos.CallGraphDelegationMode;
import com.flipkart.krystal.vajram.sql.annotations.SqlUpdate;
import java.util.List;

@Trait
@CallGraphDelegationMode(SYNC)
@SqlUpdate("UPDATE user_profile SET name = ?, email_id = ? WHERE id = ?")
public interface UpdateUserTrait extends TraitRoot<Long> {

  class _Inputs {

    @IfAbsent(FAIL)
    List<Object> parameters;
  }
}
