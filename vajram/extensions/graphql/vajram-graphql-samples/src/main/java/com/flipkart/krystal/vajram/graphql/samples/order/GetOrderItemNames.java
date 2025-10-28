package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.model.IfAbsent.IfAbsentThen;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import graphql.execution.CoercedVariables;
import graphql.execution.RawVariables;
import java.util.List;

@GraphQLFetcher
@Vajram
public abstract class GetOrderItemNames extends ComputeVajramDef<GetOrderItemNamesGraphQLResponse> {
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;
  }

  @Output
  static GetOrderItemNamesGraphQLResponse orderItemNames(OrderId id) {
    return GetOrderItemNamesGraphQLResponse.builder()
        .orderItemNames(List.of(id.value() + "_1", id.value() + "_2"))
        .name("testOrderName")
        .build();
  }
}
