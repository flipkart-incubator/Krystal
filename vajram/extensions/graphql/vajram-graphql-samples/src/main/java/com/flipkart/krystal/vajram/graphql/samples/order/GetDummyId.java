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
public abstract class GetDummyId extends ComputeVajramDef<DummyId> {
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;
  }

  @Output
  static DummyId dummyIds(String id) {
    return new DummyId(id + "_dummy_1");
  }
}
