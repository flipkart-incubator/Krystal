package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecoratorCommand;
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
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatchingDecorator implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final String instanceId;
  private final InputBatcher inputBatcher;
  private final Predicate<DependentChain> isApplicableToDependantChain;
  private final Map<ImmutableFacetValues, CompletableFuture<@Nullable Object>> futureCache =
      new LinkedHashMap<>();
  private ImmutableSet<DependentChain> activeDependentChains = ImmutableSet.of();
  private final Set<DependentChain> flushedDependentChains = new LinkedHashSet<>();

  public InputBatchingDecorator(
      String instanceId,
      InputBatcher inputBatcher,
      Predicate<DependentChain> isApplicableToDependantChain) {
    this.instanceId = instanceId;
    this.inputBatcher = inputBatcher;
    this.isApplicableToDependantChain = isApplicableToDependantChain;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    inputBatcher.onBatching(
        requests -> requests.forEach(request -> batchFacetsList(logicToDecorate, request)));
    return facetsList -> {
      ImmutableList<BatchEnabledFacetValues> immutableFacetsList =
          facetsList.stream()
              .map(
                  f -> {
                    if (f instanceof BatchEnabledFacetValues) {
                      return (BatchEnabledFacetValues) f;
                    } else {
                      throw new IllegalStateException(
                          "Expected to recieve instance of BatchEnabledFacetValues in batcher %s but received %s"
                              .formatted(instanceId, f));
                    }
                  })
              .map(BatchEnabledFacetValues::_build)
              .collect(toImmutableList());
      List<BatchedFacets> batchedFacetsList =
          immutableFacetsList.stream()
              .map(f -> f)
              .map(inputBatcher::add)
              .flatMap(Collection::stream)
              .toList();
      facetsList.forEach(
          facets ->
              futureCache.computeIfAbsent(
                  facets._build(), e -> new CompletableFuture<@Nullable Object>()));
      for (BatchedFacets batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
      return facetsList.stream()
          .collect(
              ImmutableMap
                  .<FacetValues, FacetValues, CompletableFuture<@Nullable Object>>toImmutableMap(
                      identity(),
                      key -> {
                        return requireNonNullElseGet(
                            futureCache.get(key._build()), CompletableFuture::new);
                      }));
    };
  }

  @Override
  public void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {
    if (logicDecoratorCommand instanceof InitiateActiveDepChains initiateActiveDepChains) {
      LinkedHashSet<DependentChain> allActiveDepChains =
          new LinkedHashSet<>(initiateActiveDepChains.dependantsChains());
      // Retain only the ones which are applicable for this input batching decorator
      allActiveDepChains.removeIf(isApplicableToDependantChain.negate());
      this.activeDependentChains = ImmutableSet.copyOf(allActiveDepChains);
    } else if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
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
    ImmutableMap<FacetValues, CompletableFuture<@Nullable Object>> result;
    try {
      result = logicToDecorate.execute(facetsList);
    } catch (Throwable e) {
      result = facetsList.stream().collect(toImmutableMap(identity(), i -> failedFuture(e)));
    }

    result.forEach(
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

  @Override
  public String getId() {
    return instanceId;
  }
}
