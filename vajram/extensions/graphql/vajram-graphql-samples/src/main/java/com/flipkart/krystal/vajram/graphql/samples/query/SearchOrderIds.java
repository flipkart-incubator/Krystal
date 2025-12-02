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
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Example vajram demonstrating how to use nested GraphQL input types.
 *
 * <p>This vajram accepts an {@link OrderFilterInput} parameter (which contains a nested {@link
 * DateRangeInput}) and uses it to filter and return a list of order ID strings.
 */
@GraphQLFetcher
@Vajram
public abstract class SearchOrderIds extends ComputeVajramDef<List<String>> {

  @SuppressWarnings("initialization.field.uninitialized")
  static class _Inputs {
    // Required GraphQL execution context fields
    @IfAbsent(FAIL)
    ExecutionContext graphqlExecutionContext_vg;

    @IfAbsent(FAIL)
    VajramExecutionStrategy graphqlExecutionStrategy_vg;

    @IfAbsent(FAIL)
    ExecutionStrategyParameters graphqlExecutionStrategyParams_vg;

    // The custom input type parameter with nested types
    @Nullable OrderFilterInput filter;
  }

  /**
   * Searches for orders based on the provided filter criteria.
   *
   * @param filter The filter criteria (can be null)
   * @return List of matching order ID strings
   */
  @Output
  static List<String> searchOrderIds(@Nullable OrderFilterInput filter) {
    // If no filter provided, return sample results
    if (filter == null) {
      return List.of("order_1", "order_2", "order_3");
    }

    // Example: Filter by status
    String status = filter.status();
    if (status != null && !status.isBlank()) {
      return List.of(
          "order_" + status.toLowerCase() + "_1", "order_" + status.toLowerCase() + "_2");
    }

    // Example: Filter by minimum amount
    Float minAmount = filter.minAmount();
    if (minAmount != null) {
      return List.of("order_amount_gt_" + minAmount);
    }

    // Example: Filter by date range (nested input type)
    DateRangeInput dateRange = filter.dateRange();
    if (dateRange != null) {
      String from = dateRange.from();
      String to = dateRange.to();

      // Demonstrate accessing nested input type fields
      if (from != null && to != null) {
        return List.of("order_" + from + "_to_" + to);
      } else if (from != null) {
        return List.of("order_after_" + from);
      } else if (to != null) {
        return List.of("order_before_" + to);
      }
    }

    // Default: return sample results
    return List.of("order_default");
  }
}
