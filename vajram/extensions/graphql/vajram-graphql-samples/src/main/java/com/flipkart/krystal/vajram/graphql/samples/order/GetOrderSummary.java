package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Vajram
public abstract class GetOrderSummary extends ComputeVajramDef<GetOrderSummary_GQlFields> {
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;
  }

  @Output
  static GetOrderSummary_GQlFields output() {
    OffsetDateTime currentDateTime = OffsetDateTime.now();
    LocalDate currentDate = LocalDate.now();
    return GetOrderSummary_GQlFields.builder()
        .orderItemsCount(Long.MAX_VALUE)
        .orderPlacedAt(currentDateTime)
        .orderAcceptDate(currentDate)
        .build();
  }
}
