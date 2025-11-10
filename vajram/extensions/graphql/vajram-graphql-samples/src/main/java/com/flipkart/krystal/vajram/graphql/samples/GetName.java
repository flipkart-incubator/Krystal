package com.flipkart.krystal.vajram.graphql.samples;

import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.name.Name;

@Vajram
public abstract class GetName extends ComputeVajramDef<Name> {
  static class _Inputs {
    boolean allCaps;
  }

  @Output
  static Name getName() {
    return Name_ImmutGraphQlRespJson._builder().value("Veerendra Vikramadeva");
  }
}
