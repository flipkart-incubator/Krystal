package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.Person_Id;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetPersonBanner extends ComputeVajramDef<String> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Person_Id id();
  }

  @Output
  static String outputLogic(Person_Id id) {
    return id.id() + "-bannerUrl.png";
  }
}
