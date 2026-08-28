package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

@Vajram
public abstract class GetOrderItemNames extends ComputeVajramDef<GetOrderItemNames_Fields> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();
  }

  @Output
  static GetOrderItemNames_Fields orderItemNames(Order_Id id) {
    return GetOrderItemNames_Fields_ImmutGQlResp._builder()
        .orderItemNames(
            Errable.withValue(
                List.of(Errable.withValue(id.id() + "_1"), Errable.withValue(id.id() + "_2"))))
        .nameString(Errable.withValue("testOrderName"))
        ._build();
  }
}
