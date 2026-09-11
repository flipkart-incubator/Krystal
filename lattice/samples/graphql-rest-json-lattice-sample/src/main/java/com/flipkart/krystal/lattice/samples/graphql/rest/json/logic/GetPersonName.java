package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.data.Errable.withValue;
import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name_Id;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name_Id_ImmutGQlServerResp;
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
    return Name_Id_ImmutGQlServerResp._builder()
        .firstName(withValue(id.id() + "-FirstName"))
        .lastName(id.id() + "-LastName")
        ._build();
  }
}
