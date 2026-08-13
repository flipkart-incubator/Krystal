package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

@Vajram
public abstract class GetOrderItemNames extends ComputeVajramDef<GetOrderItemNames_Model> {
  interface _Inputs {
    @IfAbsent(FAIL)
    OrderId id();
  }

  @Output
  static GetOrderItemNames_Model orderItemNames(OrderId id) {
    return GetOrderItemNames_Model_ImmutGQlResp._builder()
        .orderItemNames(
            List.of(Errable.withValue(id.value() + "_1"), Errable.withValue(id.value() + "_2")))
        .nameString(Errable.withValue("testOrderName"))
        ._build();
  }
}
