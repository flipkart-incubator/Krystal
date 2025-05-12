package com.flipkart.krystal.vajramexecutor.krystex.batching;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.OutputLogicExecutionResults;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.decoration.DecoratorCommand;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.BatchEnabledFacetValues;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatchingDecorator implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final String instanceId;
  private final InputBatcher inputBatcher;
  private final Predicate<DependentChain> isApplicableToDependentChain;
  private final Map<ImmutableFacetValues, CompletableFuture<@Nullable Object>> futureCache =
      new LinkedHashMap<>();
  private ImmutableSet<DependentChain> activeDependentChains = ImmutableSet.of();
  private final Set<DependentChain> flushedDependentChains = new LinkedHashSet<>();
  private @MonotonicNonNull OutputLogicExecutionInput outputLogicExecutionInput;

  public InputBatchingDecorator(
      String instanceId,
      InputBatcher inputBatcher,
      Predicate<DependentChain> isApplicableToDependentChain) {
    this.instanceId = instanceId;
    this.inputBatcher = inputBatcher;
    this.isApplicableToDependentChain = isApplicableToDependentChain;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    inputBatcher.onBatching(
        requests -> requests.forEach(request -> batchFacetsList(logicToDecorate, request)));
    return input -> {
      if (outputLogicExecutionInput == null) {
        outputLogicExecutionInput = input;
      }
      ImmutableList<BatchEnabledFacetValues> immutableFacetsList =
          input.facetValues().stream()
              .map(
                  f -> {
                    if (f instanceof BatchEnabledFacetValues) {
                      return (BatchEnabledFacetValues) f;
                    } else {
                      throw new IllegalStateException(
                          "Expected to receive instance of BatchEnabledFacetValues in batcher %s but received %s"
                              .formatted(instanceId, f));
                    }
                  })
              .map(BatchEnabledFacetValues::_build)
              .collect(toImmutableList());
      List<BatchedFacets> batchedFacetsList =
          immutableFacetsList.stream().map(inputBatcher::add).flatMap(Collection::stream).toList();
      input
          .facetValues()
          .forEach(
              facetValues ->
                  futureCache.computeIfAbsent(
                      facetValues._build(), e -> new CompletableFuture<@Nullable Object>()));
      for (BatchedFacets batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
      return new OutputLogicExecutionResults<>(
          input.facetValues().stream()
              .collect(
                  ImmutableMap
                      .<FacetValues, FacetValues, CompletableFuture<@Nullable Object>>
                          toImmutableMap(
                              identity(),
                              key ->
                                  requireNonNullElseGet(
                                      futureCache.get(key._build()), CompletableFuture::new))));
    };
  }

  @Override
  public void executeCommand(DecoratorCommand decoratorCommand) {
    if (decoratorCommand instanceof InitiateActiveDepChains initiateActiveDepChains) {
      LinkedHashSet<DependentChain> allActiveDepChains =
          new LinkedHashSet<>(initiateActiveDepChains.dependantsChains());
      // Retain only the ones which are applicable for this input batching decorator
      allActiveDepChains.removeIf(isApplicableToDependentChain.negate());
      this.activeDependentChains = ImmutableSet.copyOf(allActiveDepChains);
    } else if (decoratorCommand instanceof FlushCommand flushCommand) {
      flushedDependentChains.add(flushCommand.dependantsChain());
      if (flushedDependentChains.containsAll(activeDependentChains)) {
        inputBatcher.batch();
        flushedDependentChains.clear();
      }
    }
  }

  @SuppressWarnings("UnnecessaryTypeArgument") // --> To Handle nullChecker errors
  private void batchFacetsList(OutputLogic<Object> logicToDecorate, BatchedFacets batchedFacets) {
    ImmutableList<BatchEnabledFacetValues> facetsList = batchedFacets.batchItems();
    OutputLogicExecutionResults<Object> result;
    if (outputLogicExecutionInput == null) {
      if (facetsList.isEmpty()) {
        // This means the logicToDecorate (the output logic) method was never invoked
        // So we directly return empty results
        result = new OutputLogicExecutionResults<>(ImmutableMap.of());
      } else {
        throw new AssertionError(
            "The decorateLogic was never invoked by facetsList is not empty. This should not be possible");
      }
    } else {
      try {
        result = logicToDecorate.execute(outputLogicExecutionInput.withFacetValues(facetsList));
      } catch (Throwable e) {
        result =
            new OutputLogicExecutionResults<>(
                facetsList.stream().collect(toImmutableMap(identity(), i -> failedFuture(e))));
      }
    }
    result
        .results()
        .forEach(
            (inputs, resultFuture) -> {
              //noinspection RedundantTypeArguments: To Handle nullChecker errors
              linkFutures(
                  resultFuture,
                  futureCache.<CompletableFuture<@Nullable Object>>computeIfAbsent(
                      inputs._build(), request -> new CompletableFuture<@Nullable Object>()));
            });
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    inputBatcher.onConfigUpdate(
        new NestedConfig(String.format("input_batching.%s.", instanceId), configProvider));
  }
}
