package com.flipkart.krystal.vajram.graphql.samples.order;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Vajram
public abstract class GetOrderSummary extends ComputeVajramDef<GetOrderSummary_GQlFields> {

  /** Unix epoch - January 1, 1970 00:00:00 UTC */
  public static final OffsetDateTime UNIX_EPOCH_DATE_TIME =
      OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

  /** Unix epoch date - January 1, 1970 */
  public static final LocalDate UNIX_EPOCH_DATE = LocalDate.of(1970, 1, 1);

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL)
    OrderId id;
  }

  @Output
  static GetOrderSummary_GQlFields output() {
    return GetOrderSummary_GQlFields.builder()
        .orderItemsCount(Long.MAX_VALUE)
        .orderPlacedAt(UNIX_EPOCH_DATE_TIME)
        .orderAcceptDate(UNIX_EPOCH_DATE)
        .build();
  }
}
