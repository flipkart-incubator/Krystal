package com.flipkart.krystal.lattice.samples.graphql.rest.json.logic;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.lattice.samples.graphql.rest.json.logic.person.PersonId;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetImageDataOfPerson
    extends ComputeVajramDef<GetImageDataOfPerson_GQlFields> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    PersonId id;
  }

  @Output
  static GetImageDataOfPerson_GQlFields outputLogic(PersonId id) {
    return GetImageDataOfPerson_GQlFields.builder()
        .mainUrl(id.value() + "-mainUrl.png")
        .thumbnailUrl(id.value() + "-thumbnailUrl.png")
        .build();
  }
}
