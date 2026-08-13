package com.flipkart.krystal.vajram.graphql.samples;

import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.name.Name_Id;
import com.flipkart.krystal.vajram.graphql.samples.name.Name_Id_ImmutGQlResp;

@Vajram
public abstract class GetName extends ComputeVajramDef<Name_Id> {

  @Output
  static Name_Id getName() {
    return Name_Id_ImmutGQlResp._builder().value("value").string("string")._build();
  }
}
