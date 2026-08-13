package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.account.Account_Id;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.Person_Id;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.Person_Id_ImmutGQlResp;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetOwnerOfAccount extends ComputeVajramDef<Person_Id> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Account_Id id();
  }

  @Output
  static Person_Id outputLogic(Account_Id id) {
    return Person_Id_ImmutGQlResp._builder().id("PRSN" + id.id())._build();
  }
}
