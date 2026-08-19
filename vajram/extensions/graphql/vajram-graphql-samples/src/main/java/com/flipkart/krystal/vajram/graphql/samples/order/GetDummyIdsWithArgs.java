package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id;
import com.flipkart.krystal.vajram.graphql.samples.dummy.Dummy_Id_ImmutGQlResp;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@Vajram
public abstract class GetDummyIdsWithArgs extends ComputeVajramDef<List<Dummy_Id>> {
  interface _Inputs {
    @IfAbsent(FAIL)
    Order_Id id();

    boolean filter();

    String preferredType();

    int count();
  }

  @Output
  static List<Dummy_Id> dummyIds(
      Order_Id id, @Nullable String preferredType, @Nullable Integer count) {
    if ("boom".equals(preferredType)) {
      throw new RuntimeException("GetDummyIdsWithArgs failed for preferredType=boom");
    }
    List<Dummy_Id> ids = new ArrayList<>();
    for (int i = 1; i <= (count == null ? 1 : count); i++) {
      ids.add(Dummy_Id_ImmutGQlResp._builder().dummyId(id.id() + "_dummy_" + i)._build());
    }
    return ids;
  }
}
