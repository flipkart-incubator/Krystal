package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.utils.Futures.linkFutures;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableFacets;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.BatchableImmutableFacets;
import com.flipkart.krystal.vajram.batching.BatchableSupplier;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.BatchableFacets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatchingDecorator<
        I /*BatchableInputs*/ extends Facets, C /*CommonFacets*/ extends Facets>
    implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final String instanceId;
  private final InputBatcher<I, C> inputBatcher;
  private final BatchableSupplier<I, C> batchableSupplier;
  private final Predicate<DependantChain> isApplicableToDependantChain;
  private final Map<ImmutableFacets, CompletableFuture<@Nullable Object>> futureCache =
      new LinkedHashMap<>();
  private ImmutableSet<DependantChain> activeDependantChains = ImmutableSet.of();
  private final Set<DependantChain> flushedDependantChains = new LinkedHashSet<>();

  public InputBatchingDecorator(
      String instanceId,
      InputBatcher<I, C> inputBatcher,
      BatchableSupplier<I, C> batchableSupplier,
      Predicate<DependantChain> isApplicableToDependantChain) {
    this.instanceId = instanceId;
    this.inputBatcher = inputBatcher;
    this.batchableSupplier = batchableSupplier;
    this.isApplicableToDependantChain = isApplicableToDependantChain;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    inputBatcher.onBatching(
        requests -> requests.forEach(request -> batchFacetsList(logicToDecorate, request)));
    return facetsList -> {
      ImmutableList<BatchableImmutableFacets<I, C>> immutableFacetsList =
          facetsList.stream()
              .map(
                  f -> {
                    if (f instanceof BatchableFacets) {
                      //noinspection unchecked
                      return (BatchableFacets<I, C>) f;
                    } else {
                      throw new IllegalStateException(
                          "Expected to recieve instance of BatchableFacets in batcher %s but received %s"
                              .formatted(
                                  instanceId,
                                  Optional.ofNullable(f)
                                      .<Class<?>>map(Object::getClass)
                                      .orElse(Void.class)));
                    }
                  })
              .map(BatchableFacets::_build)
              .collect(toImmutableList());
      List<BatchedFacets<I, C>> batchedFacetsList =
          immutableFacetsList.stream()
              .map(f -> (BatchableFacets<I, C>) f)
              .map(unbatched -> inputBatcher.add(unbatched._batchable(), unbatched._common()))
              .flatMap(Collection::stream)
              .toList();
      facetsList.forEach(
          facets ->
              futureCache.computeIfAbsent(
                  facets._build(), e -> new CompletableFuture<@Nullable Object>()));
      for (BatchedFacets<I, C> batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
      return facetsList.stream()
          .collect(
              ImmutableMap.<Facets, Facets, CompletableFuture<@Nullable Object>>toImmutableMap(
                  Function.identity(),
                  key ->
                      Optional.ofNullable(futureCache.get(key._build()))
                          .orElseThrow(
                              () ->
                                  new AssertionError(
                                      "Future cache has been primed with values. This should never happen"))));
    };
  }

  @Override
  public void executeCommand(LogicDecoratorCommand logicDecoratorCommand) {
    if (logicDecoratorCommand instanceof InitiateActiveDepChains initiateActiveDepChains) {
      LinkedHashSet<DependantChain> allActiveDepChains =
          new LinkedHashSet<>(initiateActiveDepChains.dependantsChains());
      // Retain only the ones which are applicable for this input batching decorator
      allActiveDepChains.removeIf(isApplicableToDependantChain.negate());
      this.activeDependantChains = ImmutableSet.copyOf(allActiveDepChains);
    } else if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
      flushedDependantChains.add(flushCommand.dependantsChain());
      if (flushedDependantChains.containsAll(activeDependantChains)) {
        inputBatcher.batch();
        flushedDependantChains.clear();
      }
    }
  }

  private void batchFacetsList(
      OutputLogic<Object> logicToDecorate, BatchedFacets<I, C> batchedFacets) {
    ImmutableList<BatchableFacets<I, C>> requests =
        batchedFacets.batch().stream()
            .map(each -> batchableSupplier.createBatchable(each, batchedFacets.common()))
            .collect(toImmutableList());
    logicToDecorate
        .execute(requests)
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

  @Override
  public String getId() {
    return instanceId;
  }
}
