package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;

/**
 * Single-field, arg-bearing scalar data fetcher. Used to test that arg-bearing single-field
 * {@code @dataFetcher} fields fan out one request per GraphQL alias.
 */
@Vajram
public abstract class GetOrderItemAt extends ComputeVajramDef<String> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();

    @IfAbsent(FAIL)
    int index();
  }

  @Output
  static String output(Order_Id id, int index) {
    return id.id() + "_item_" + index;
  }
}
