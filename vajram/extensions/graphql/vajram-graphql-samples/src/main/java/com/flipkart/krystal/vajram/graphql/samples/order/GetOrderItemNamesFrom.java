package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.util.List;

/**
 * Single-field, arg-bearing list data fetcher. Used to test that arg-bearing single-field
 * {@code @dataFetcher} list fields fan out one request per GraphQL alias.
 */
@Vajram
public abstract class GetOrderItemNamesFrom extends ComputeVajramDef<List<String>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();

    @IfAbsent(FAIL)
    int offset();
  }

  @Output
  static List<String> output(Order_Id id, int offset) {
    return List.of(id.id() + "_from_" + offset + "_1", id.id() + "_from_" + offset + "_2");
  }
}
