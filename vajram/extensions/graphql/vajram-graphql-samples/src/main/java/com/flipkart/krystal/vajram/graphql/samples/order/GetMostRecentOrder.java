package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetMostRecentOrder extends ComputeVajramDef<Order_Id> {
  interface _Inputs {
    @IfAbsent(FAIL)
    String userId();
  }

  @Output
  static Order_Id mostRecentOrder(String userId) {
    return Order_Id_ImmutGQlServerResp._builder().id("MostRecentOrderOf_" + userId)._build();
  }
}
