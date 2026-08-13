package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name_Id;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name_Id_ImmutGQlResp;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.Person_Id;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetPersonName extends ComputeVajramDef<Name_Id> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Person_Id id();
  }

  @Output
  static Name_Id outputLogic(Person_Id id) {
    return Name_Id_ImmutGQlResp._builder()
        .firstName(id.id() + "-FirstName")
        .lastName(id.id() + "-LastName")
        ._build();
  }
}
