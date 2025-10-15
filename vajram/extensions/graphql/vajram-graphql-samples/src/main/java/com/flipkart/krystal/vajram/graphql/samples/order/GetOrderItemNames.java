package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import java.util.List;

@GraphQLFetcher
@Vajram
public abstract class GetOrderItemNames extends ComputeVajramDef<List<String>> {
  static class _Inputs {
    @IfAbsent(FAIL)
    String orderId;
  }

  @Output
  static List<String> orderItemNames(String orderId) {
    return List.of(orderId + "_1", orderId + "_2");
  }
}
