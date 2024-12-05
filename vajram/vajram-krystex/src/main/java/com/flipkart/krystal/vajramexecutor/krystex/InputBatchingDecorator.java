package com.flipkart.krystal.vajramexecutor.krystex;

import static com.flipkart.krystal.concurrent.Futures.linkFutures;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.function.Function.identity;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.kryon.DependantChain;
import com.flipkart.krystal.krystex.logicdecoration.FlushCommand;
import com.flipkart.krystal.krystex.logicdecoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecoratorCommand;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.FacetsConverter;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.flipkart.krystal.vajram.batching.UnBatchedFacets;
import com.flipkart.krystal.vajram.facets.FacetValuesAdaptor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatchingDecorator<
        I /*BatchableInputs*/ extends FacetValuesAdaptor,
        C /*CommonFacets*/ extends FacetValuesAdaptor>
    implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();
  private final String instanceId;
  private final InputBatcher<I, C> inputBatcher;
  private final FacetsConverter<I, C> facetsConverter;
  private final Predicate<DependantChain> isApplicableToDependantChain;
  private final Map<Facets, CompletableFuture<@Nullable Object>> futureCache = new HashMap<>();
  private ImmutableSet<DependantChain> activeDependantChains = ImmutableSet.of();
  private final Set<DependantChain> flushedDependantChains = new LinkedHashSet<>();

  public InputBatchingDecorator(
      String instanceId,
      InputBatcher<I, C> inputBatcher,
      FacetsConverter<I, C> facetsConverter,
      Predicate<DependantChain> isApplicableToDependantChain) {
    this.instanceId = instanceId;
    this.inputBatcher = inputBatcher;
    this.facetsConverter = facetsConverter;
    this.isApplicableToDependantChain = isApplicableToDependantChain;
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate, OutputLogicDefinition<Object> originalLogicDefinition) {
    inputBatcher.onBatching(
        requests -> requests.forEach(request -> batchFacetsList(logicToDecorate, request)));
    return facetsList -> {
      List<UnBatchedFacets<I, C>> requests =
          facetsList.stream()
              .map(
                  facets ->
                      new UnBatchedFacets<>(
                          facetsConverter.getBatched(facets), facetsConverter.getCommon(facets)))
              .toList();
      List<BatchedFacets<I, C>> batchedFacetsList =
          requests.stream()
              .map(
                  unbatchedInput ->
                      inputBatcher.add(
                          unbatchedInput.batchedInputs(), unbatchedInput.commonFacets()))
              .flatMap(Collection::stream)
              .toList();
      requests.forEach(
          request ->
              futureCache.computeIfAbsent(
                  request.toFacetValues(), e -> new CompletableFuture<@Nullable Object>()));
      for (BatchedFacets<I, C> batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
      return requests.stream()
          .map(UnBatchedFacets::toFacetValues)
          .collect(
              ImmutableMap.<Facets, Facets, CompletableFuture<@Nullable Object>>toImmutableMap(
                  identity(),
                  key ->
                      Optional.ofNullable(futureCache.get(key))
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

  @SuppressWarnings("UnnecessaryTypeArgument") // To Handle nullChecker errors
  private void batchFacetsList(
      OutputLogic<Object> logicToDecorate, BatchedFacets<I, C> batchedFacets) {
    ImmutableList<UnBatchedFacets<I, C>> requests =
        batchedFacets.batch().stream()
            .map(each -> new UnBatchedFacets<>(each, batchedFacets.commonFacets()))
            .collect(toImmutableList());
    ImmutableMap<Facets, CompletableFuture<@Nullable Object>> result;
    ImmutableList<Facets> facetsList =
        requests.stream().map(UnBatchedFacets::toFacetValues).collect(toImmutableList());
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
                  inputs, request -> new CompletableFuture<@Nullable Object>()));
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
