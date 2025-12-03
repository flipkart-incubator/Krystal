package com.flipkart.krystal.vajram.graphql.samples.query;

import static com.flipkart.krystal.model.IfAbsent.IfAbsentThen.FAIL;

import com.flipkart.krystal.model.IfAbsent;
import com.flipkart.krystal.vajram.ComputeVajramDef;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.graphql.api.GraphQLFetcher;
import com.flipkart.krystal.vajram.graphql.api.VajramExecutionStrategy;
import com.flipkart.krystal.vajram.graphql.samples.dummy.DummyId;
import com.flipkart.krystal.vajram.graphql.samples.input.DummyFilterInput;
import graphql.execution.ExecutionContext;
import graphql.execution.ExecutionStrategyParameters;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Example vajram demonstrating how to use GraphQL input types.
 *
 * <p>This vajram accepts a {@link DummyFilterInput} parameter and uses it to filter and return a
 * list of DummyIds. The aggregator will then use these IDs to fetch the full Dummy entities.
 */
@GraphQLFetcher
@Vajram
public abstract class SearchDummies extends ComputeVajramDef<List<DummyId>> {

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
    @Nullable DummyFilterInput filter;
  }

  /**
   * Searches for dummies based on the provided filter criteria.
   *
   * @param filter The filter criteria (can be null)
   * @return List of matching DummyIds
   */
  @Output
  static List<DummyId> searchDummies(@Nullable DummyFilterInput filter) {
    // If no filter provided, return sample results
    if (filter == null) {
      return List.of(new DummyId("dummy_1"), new DummyId("dummy_2"), new DummyId("dummy_3"));
    }

    // Example: Filter by name
    String name = filter.name();
    if (name != null && !name.isBlank()) {
      return List.of(new DummyId("dummy_" + name.toLowerCase()));
    }

    // Example: Filter by age range
    Integer minAge = filter.minAge();
    Integer maxAge = filter.maxAge();
    if (minAge != null || maxAge != null) {
      return List.of(new DummyId("dummy_age_" + minAge + "_to_" + maxAge));
    }

    // Example: Filter by preferred types
    List<String> preferredTypes = filter.preferredTypes();
    if (preferredTypes != null && !preferredTypes.isEmpty()) {
      return preferredTypes.stream()
          .map(type -> new DummyId("dummy_type_" + type.toLowerCase()))
          .toList();
    }

    // Example: Include inactive flag
    Boolean includeInactive = filter.includeInactive();
    if (includeInactive != null && includeInactive) {
      return List.of(new DummyId("dummy_1"), new DummyId("dummy_inactive_1"));
    }

    // Default: return sample results
    return List.of(new DummyId("dummy_default"));
  }
}
