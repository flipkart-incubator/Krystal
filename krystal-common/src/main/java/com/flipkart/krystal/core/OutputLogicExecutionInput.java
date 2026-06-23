package com.flipkart.krystal.core;

import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.data.ExecutionItem;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** A wrapper class for all the data that is needed by the output logic of a vajram */
public final class OutputLogicExecutionInput {
  private final List<ExecutionItem> executionItems;
  private final ExecutorService graphExecutor;
  @Getter private final ContextEnricher contextEnricher;
  private CompletableFuture @MonotonicNonNull [] responseFutures;

  public OutputLogicExecutionInput(
      List<ExecutionItem> executionItems,
      ExecutorService graphExecutor,
      ContextEnricher contextEnricher) {
    this.executionItems = unmodifiableList(executionItems);
    this.graphExecutor = graphExecutor;
    this.contextEnricher = contextEnricher;
  }

  public OutputLogicExecutionInput withExecutionItems(List<ExecutionItem> facetValues) {
    return new OutputLogicExecutionInput(facetValues, graphExecutor(), contextEnricher);
  }

  public List<ExecutionItem> executionItems() {
    return executionItems;
  }

  public CompletableFuture[] responseFutures() {
    if (responseFutures == null) {
      CompletableFuture[] responseFutures = new CompletableFuture[executionItems.size()];
      for (int i = 0; i < executionItems.size(); i++) {
        responseFutures[i] = executionItems.get(i).response();
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
