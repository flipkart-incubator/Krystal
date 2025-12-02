package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.name.Name_ImmutGQlRespJson;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.PersonId;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetPersonName extends ComputeVajramDef<Name> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    PersonId id;
  }

  @Output
  static Name outputLogic(PersonId id) {
    return Name_ImmutGQlRespJson._builder()
        .firstName(id.value() + "-FirstName")
        .lastName(id.value() + "-LastName");
  }
}
