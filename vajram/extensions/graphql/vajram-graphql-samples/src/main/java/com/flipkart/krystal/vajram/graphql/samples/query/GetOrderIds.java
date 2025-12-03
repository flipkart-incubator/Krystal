package com.flipkart.krystal.vajram.graphql.samples.query;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.input.DateRangeInput;
import com.flipkart.krystal.vajram.graphql.samples.input.OrderFilterInput;
import com.flipkart.krystal.vajram.graphql.samples.order.OrderId;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Example vajram demonstrating how to use input types with entity responses.
 *
 * <p>This vajram accepts an {@link OrderFilterInput} parameter and returns a list of {@link
 * OrderId}s. The GraphQL aggregator will automatically use these IDs to fetch the full Order
 * entities.
 *
 * <p>This demonstrates the complete flow: input type → vajram returns IDs → aggregator fetches
 * entities
 */
@GraphQLFetcher
@Vajram
public abstract class GetOrderIds extends ComputeVajramDef<List<OrderId>> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    // Required GraphQL execution context fields
    @IfAbsent(FAIL)
    ExecutionContext graphqlExecutionContext_vg;

    @IfAbsent(FAIL)
    VajramExecutionStrategy graphqlExecutionStrategy_vg;

    @IfAbsent(FAIL)
    ExecutionStrategyParameters graphqlExecutionStrategyParams_vg;

    // The custom input type parameter
    @Nullable OrderFilterInput filter;
  }

  /**
   * Fetches order IDs based on the provided filter criteria.
   *
   * <p>This is a sample implementation showing how to:
   *
   * <ol>
   *   <li>Accept an input type parameter
   *   <li>Extract and use filter fields (including nested types)
   *   <li>Return order IDs (the aggregator will fetch full entities)
   * </ol>
   *
   * @param filter The filter criteria (can be null)
   * @return List of Order IDs
   */
  @Output
  static List<OrderId> getOrderIds(@Nullable OrderFilterInput filter) {
    List<OrderId> orderIds = new ArrayList<>();

    // If no filter provided, return sample order IDs
    if (filter == null) {
      orderIds.add(new OrderId("order_1"));
      orderIds.add(new OrderId("order_2"));
      orderIds.add(new OrderId("order_3"));
      return orderIds;
    }

    // Extract filter fields
    String status = filter.status();
    Float minAmount = filter.minAmount();
    DateRangeInput dateRange = filter.dateRange();

    // Example: Filter by status
    if (status != null && !status.isBlank()) {
      orderIds.add(new OrderId("order_" + status.toLowerCase() + "_1"));
      orderIds.add(new OrderId("order_" + status.toLowerCase() + "_2"));
      return orderIds;
    }

    // Example: Filter by minimum amount
    if (minAmount != null) {
      orderIds.add(new OrderId("order_expensive_1"));
      orderIds.add(new OrderId("order_expensive_2"));
      return orderIds;
    }

    // Example: Filter by date range (nested input type)
    if (dateRange != null) {
      String from = dateRange.from();
      String to = dateRange.to();

      // Demonstrate accessing nested input type fields
      if (from != null && to != null) {
        orderIds.add(new OrderId("order_" + from + "_to_" + to + "_1"));
        orderIds.add(new OrderId("order_" + from + "_to_" + to + "_2"));
        return orderIds;
      } else if (from != null) {
        orderIds.add(new OrderId("order_after_" + from));
        return orderIds;
      } else if (to != null) {
        orderIds.add(new OrderId("order_before_" + to));
        return orderIds;
      }
    }

    // Default: return sample order IDs
    orderIds.add(new OrderId("order_default_1"));
    orderIds.add(new OrderId("order_default_2"));
    return orderIds;
  }
}

