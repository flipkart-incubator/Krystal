package com.flipkart.krystal.vajramexecutor.krystex.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.facets.Dependency;
import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

@UtilityClass
public class DispatchTargetComputing {

  /**
   * Functional interface to encapsulate dispatch target computation logic
   *
   * @param <T> The request type of the trait for which dispatch target is being computed
   */
  public sealed interface DispatchTargetComputer<T extends Request<?>> {

    /**
     * Computes the dispatch target for a trait
     *
     * @param dependency The dependency facet which depends on the trait. null if the request is
     *     coming from outside the Krystal graph
     * @param request The request sent to the trait
     * @return The vajramId or the request type class of the dispatch target vajram
     */
    @Nullable Object computeDispatchTarget(@Nullable Dependency dependency, T request);
  }

  /**
   * Computes the dispatch target by returning the vajramId. This is useful if the developers have
   * defined the dispatch target vajramId strings outside java code (in a config file or DB, etc.),
   * as the vajramId can be returned as-is without having to map the vajramId string to a class
   * object.
   *
   * @param <T> The request type of the trait for which dispatch target is being computed
   */
  @FunctionalInterface
  public non-sealed interface DispatchTargetIdComputer<T extends Request<?>>
      extends DispatchTargetComputer<T> {
    /**
     * Computes the dispatch target for a trait
     *
     * @param dependency The dependency facet which depends on the trait. null if the request is
     *     coming from outside the Krystal graph
     * @param request The request sent to the trait
     * @return The vajramId of the dispatch target vajram
     */
    @Nullable VajramID computeDispatchTarget(@Nullable Dependency dependency, T request);
  }

  /**
   * Computes the dispatch target by returning the request class type. This is useful if the
   * developers have mapped coded the dispatch targets in java code - returning request type is more
   * type safe and convenient.
   *
   * @param <T> The request type of the trait for which dispatch target is being computed
   */
  @FunctionalInterface
  public non-sealed interface DispatchTargetReqTypeComputer<T extends Request<?>>
      extends DispatchTargetComputer<T> {
    /**
     * Computes the dispatch target for a trait
     *
     * @param dependency The dependency facet which depends on the trait. null if the request is
     *     coming from outside the Krystal graph
     * @param request The request sent to the trait
     * @return The request type class of the dispatch target vajram
     */
    @Nullable Class<? extends T> computeDispatchTarget(@Nullable Dependency dependency, T request);
  }
}
