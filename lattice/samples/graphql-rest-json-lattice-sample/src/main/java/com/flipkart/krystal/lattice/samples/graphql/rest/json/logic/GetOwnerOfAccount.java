package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.account.AccountId;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.PersonId;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetOwnerOfAccount extends ComputeVajramDef<PersonId> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    AccountId id;
  }

  @Output
  static PersonId outputLogic(AccountId id) {
    return new PersonId("PRSN" + id.value());
  }
}
