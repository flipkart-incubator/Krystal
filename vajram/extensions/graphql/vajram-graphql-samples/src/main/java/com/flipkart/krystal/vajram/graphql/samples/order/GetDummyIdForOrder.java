package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import com.flipkart.krystal.vajram.graphql.samples.dummy.DummyId;

@GraphQLFetcher
@Vajram
public abstract class GetDummyIdForOrder extends ComputeVajramDef<DummyId> {
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;

    String name;
  }

  @Output
  static DummyId dummyIds(OrderId id) {
    return new DummyId(id.value() + "_dummy_1");
  }
}
