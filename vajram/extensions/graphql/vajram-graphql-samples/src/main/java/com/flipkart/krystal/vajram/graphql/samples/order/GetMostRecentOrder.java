package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

@Vajram
public abstract class GetMostRecentOrder extends ComputeVajramDef<OrderId> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String userId;
  }

  @Output
  static OrderId mostRecentOrder(String userId) {
    return new OrderId("MostRecentOrderOf_" + userId);
  }
}
