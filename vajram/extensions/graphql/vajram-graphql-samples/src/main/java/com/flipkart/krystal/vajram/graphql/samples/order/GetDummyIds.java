package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id_ImmutGQlServerResp;
import java.util.List;

@Vajram
public abstract class GetDummyIds extends ComputeVajramDef<List<Dummy_Id>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();
  }

  @Output
  static List<Dummy_Id> dummyIds(Order_Id id) {
    if ("orderBadDummies".equals(id.id())) {
      throw new RuntimeException("GetDummyIds failed for id=orderBadDummies");
    }
    return List.of(Dummy_Id_ImmutGQlServerResp._builder().dummyId(id.id() + "_dummy_1")._build());
  }
}
