package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.Person_Id;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetImageDataOfPerson extends ComputeVajramDef<GetImageDataOfPerson_Fields> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Person_Id id();
  }

  @Output
  static GetImageDataOfPerson_Fields outputLogic(Person_Id id) {
    return GetImageDataOfPerson_Fields_ImmutGQlResp._builder()
        .mainUrl(Errable.withValue(id.id() + "-mainUrl.png"))
        .thumbnailUrl(Errable.withValue(id.id() + "-thumbnailUrl.png"))
        ._build();
  }
}
