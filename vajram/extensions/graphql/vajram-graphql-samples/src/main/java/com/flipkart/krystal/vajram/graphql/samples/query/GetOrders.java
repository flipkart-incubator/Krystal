package com.flipkart.krystal.vajram.graphql.samples.query;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.input.OrderFilterInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Example vajram demonstrating how to use input types with entity responses via a wrapper type.
 *
 * <p>This vajram accepts an {@link OrderFilterInput} parameter and returns an OrdersResultId which
 * will be used by the aggregator to fetch the OrdersResult.
 *
 * <p>This demonstrates: input type → vajram → wrapper result type → nested entities
 */
@GraphQLFetcher
@Vajram
public abstract class GetOrders extends ComputeVajramDef<OrdersResultId> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    @IfAbsent(FAIL) ExecutionContext graphqlExecutionContext_vg;
    @IfAbsent(FAIL) VajramExecutionStrategy graphqlExecutionStrategy_vg;
    @IfAbsent(FAIL) ExecutionStrategyParameters graphqlExecutionStrategyParams_vg;

    // The custom input type parameter - this demonstrates input type usage!
    @Nullable OrderFilterInput input;
  }

  /**
   * Returns an OrdersResultId containing the filter to be used for fetching orders.
   *
   * <p>This vajram creates a result ID that encodes the filter criteria. The nested vajram {@link
   * GetOrderIdsForResult} will then use this ID to fetch the actual order IDs.
   *
   * @param input The filter criteria (can be null)
   * @return OrdersResultId containing encoded filter
   */
  @Output
  static OrdersResultId getOrders(@Nullable OrderFilterInput input) {
    // Create a result ID that encodes the filter
    // In a real implementation, this might be a cache key or session ID
    return new OrdersResultId(serializeFilter(input));
  }

  /**
   * Helper to serialize the filter into a string ID.
   *
   * @param input The filter
   * @return Serialized filter string
   */
  private static String serializeFilter(@Nullable OrderFilterInput input) {
    if (input == null) {
      return "no_filter";
    }

    StringBuilder sb = new StringBuilder();
    if (input.status() != null) {
      sb.append("status:").append(input.status()).append(";");
    }
    if (input.minAmount() != null) {
      sb.append("minAmount:").append(input.minAmount()).append(";");
    }
    if (input.dateRange() != null) {
      if (input.dateRange().from() != null) {
        sb.append("from:").append(input.dateRange().from()).append(";");
      }
      if (input.dateRange().to() != null) {
        sb.append("to:").append(input.dateRange().to()).append(";");
      }
    }
    return sb.length() > 0 ? sb.toString() : "empty_filter";
  }
}

