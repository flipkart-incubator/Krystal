package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id_ImmutGQlResp;

@Vajram
public abstract class GetDummyIdForOrder extends ComputeVajramDef<Dummy_Id> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();

    String name();
  }

  @Output
  static Dummy_Id dummyIds(Order_Id id) {
    return Dummy_Id_ImmutGQlResp._builder().dummyId(id.id() + "_dummy_1")._build();
  }
}
