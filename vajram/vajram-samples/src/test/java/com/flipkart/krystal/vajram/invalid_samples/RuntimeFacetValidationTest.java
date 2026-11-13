package com.flipkart.krystal.vajram.invalid_samples;

import static com.flipkart.krystal.vajram.samples.Util.loadFromClasspath;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.flipkart.krystal.vajram.invalid_samples.loop.InvalidLoopVajram;
import com.flipkart.krystal.vajram.invalid_samples.unannotated.InvalidUnannotatedDepVajram;
import com.flipkart.krystal.vajram.samples.calculator.add.Add;
import org.junit.jupiter.api.Test;

/**
 * Verifies the graph-load-time validations in {@code VajramDefinition} for violations which are
 * defined via {@code getSimpleInputResolvers()} and hence cannot be caught by the compile time
 * checks in the annotation processor.
 */
class RuntimeFacetValidationTest {

  @Test
  void facetLoopViaSimpleInputResolvers_failsGraphLoading() {
    assertThatThrownBy(
            () ->
                loadFromClasspath(
                        Add.class.getPackageName(), InvalidLoopVajram.class.getPackageName())
                    .build())
        .hasStackTraceContaining("Cycle detected in facet dependencies")
        .hasStackTraceContaining("InvalidLoopVajram")
        .hasStackTraceContaining("→");
  }

  @Test
  void nonBatchedDependingOnBatched_viaSimpleInputResolvers_failsGraphLoading() {
    assertThatThrownBy(
            () ->
                loadFromClasspath(
                        Add.class.getPackageName(),
                        InvalidUnannotatedDepVajram.class.getPackageName())
                    .build())
        .hasStackTraceContaining("Facet 'lookup'")
        .hasStackTraceContaining("InvalidUnannotatedDepVajram")
        .hasStackTraceContaining("cannot depend on @Batched facet 'id'");
  }
}
