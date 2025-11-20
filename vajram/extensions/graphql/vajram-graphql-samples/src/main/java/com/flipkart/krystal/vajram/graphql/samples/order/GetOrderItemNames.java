package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

@Vajram
public abstract class GetOrderItemNames extends ComputeVajramDef<GetOrderItemNames_GQlFields> {
  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;
  }

  @Output
  static GetOrderItemNames_GQlFields orderItemNames(OrderId id) {
    return GetOrderItemNames_GQlFields.builder()
        .orderItemNames(List.of(id.value() + "_1", id.value() + "_2"))
        .nameString("testOrderName")
        .build();
  }
}
