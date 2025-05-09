package com.flipkart.krystal.core;

import com.flipkart.krystal.data.FacetValues;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * A wrapper class for all the data that is needed by the output logic of a vajram
 *
 * @param facetValues The facets to be used for executing the output logic
 * @param graphExecutor The executor service which is used to execute the krystal graph. Note that
 *     this might be an event loop executor, so no blocking operations are to performed in this.
 *     This useful, for example, in IOVajrams when a piece of logic (Output.unbatch, for example)
 *     needs to be executed in the same thread in which the executor service is running since it has
 *     the relevant logging context etc. configured.
 */
public record OutputLogicExecutionInput(
    ImmutableList<? extends FacetValues> facetValues, ExecutorService graphExecutor) {
  public OutputLogicExecutionInput withFacetValues(List<? extends FacetValues> facetValues) {
    return new OutputLogicExecutionInput(ImmutableList.copyOf(facetValues), graphExecutor());
  }
}
