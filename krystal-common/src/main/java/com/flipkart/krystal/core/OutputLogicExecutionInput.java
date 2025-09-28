package com.flipkart.krystal.core;

import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.data.ExecutionItem;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** A wrapper class for all the data that is needed by the output logic of a vajram */
public final class OutputLogicExecutionInput {
  private final List<ExecutionItem> facetValueResponses;
  private final ExecutorService graphExecutor;
  private CompletableFuture @MonotonicNonNull [] responseFutures;

  public OutputLogicExecutionInput(
      List<ExecutionItem> facetValueResponses, ExecutorService graphExecutor) {
    facetValueResponses = unmodifiableList(facetValueResponses);
    this.facetValueResponses = facetValueResponses;
    this.graphExecutor = graphExecutor;
  }

  public OutputLogicExecutionInput withFacetValueResponses(List<ExecutionItem> facetValues) {
    return new OutputLogicExecutionInput(facetValues, graphExecutor());
  }

  public List<ExecutionItem> facetValueResponses() {
    return facetValueResponses;
  }

  public CompletableFuture[] responseFutures() {
    if (responseFutures == null) {
      CompletableFuture[] responseFutures = new CompletableFuture[facetValueResponses.size()];
      for (int i = 0; i < facetValueResponses.size(); i++) {
        responseFutures[i] = facetValueResponses.get(i).response();
      }
      this.responseFutures = responseFutures;
      return responseFutures;
    }
    return responseFutures;
  }

  public ExecutorService graphExecutor() {
    return graphExecutor;
  }
}
