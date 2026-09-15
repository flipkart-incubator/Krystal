package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id_ImmutGQlServerResp;
import org.checkerframework.checker.nullness.qual.Nullable;

@Vajram
public abstract class GetDummyIdForOrder extends ComputeVajramDef<Dummy_Id> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();

    String name();
  }

  @Output
  static Dummy_Id dummyIds(Order_Id id, @Nullable String name) {
    if ("boom".equals(name)) {
      throw new RuntimeException("GetDummyIdForOrder failed for name=boom");
    }
    return Dummy_Id_ImmutGQlServerResp._builder().dummyId(id.id() + "_dummy_1")._build();
  }
}
