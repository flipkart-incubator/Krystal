package com.flipkart.krystal.krystex.batching;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.FlushableDecorator;
import com.flipkart.krystal.krystex.decoration.InitiableWithActiveDepChains;
import com.flipkart.krystal.krystex.decoration.InitiateActiveDepChains;
import com.flipkart.krystal.krystex.epochs.EpochGroup;
import com.flipkart.krystal.krystex.epochs.VajramEpochGroups;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.BatchEnabledFacetValues;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class InputBatchingDecorator
    implements OutputLogicDecorator, FlushableDecorator, InitiableWithActiveDepChains {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final List<InputBatcher> sharedInputBatchersByEpoch;
  private final Map<DependentChain, InputBatcher> simpleInputBatchersByDepChain =
      new LinkedHashMap<>();
  private final Supplier<InputBatcher> inputBatcherFactory;
  private final ImmutableMap<Integer, EpochGroup> depChainsByEpoch;
  private final Map<DependentChain, Integer> epochByDepChain = new LinkedHashMap<>();
  private final List<Set<DependentChain>> dependentChainsToFlushByEpoch = new ArrayList<>();
  private @MonotonicNonNull OutputLogicExecutionInput outputLogicExecutionInput;

  public InputBatchingDecorator(
      Supplier<InputBatcher> inputBatcherFactory, VajramEpochGroups vajramEpochGroups) {
    this.inputBatcherFactory = inputBatcherFactory;
    ImmutableMap<Integer, EpochGroup> depChainsByEpoch = vajramEpochGroups.depChainsByEpochGroup();
    this.depChainsByEpoch = depChainsByEpoch;
    this.sharedInputBatchersByEpoch = new ArrayList<>(depChainsByEpoch.size());
    int localEpoch = 0;
    for (var epochGroup : depChainsByEpoch.values()) {
      this.sharedInputBatchersByEpoch.add(inputBatcherFactory.get());
      for (DependentChain dependentChain : epochGroup.dependentChains()) {
        epochByDepChain.put(dependentChain, localEpoch);
      }
      localEpoch++;
    }
  }

  @Override
  public OutputLogic<Object> decorateLogic(
      OutputLogic<Object> logicToDecorate,
      OutputLogicDefinition<Object> originalLogicDefinition,
      LogicExecutionContext context) {
    DependentChain dependentChain = context.dependentChain();
    InputBatcher inputBatcher = getInputBatcher(dependentChain);
    inputBatcher.onBatching(
        requests -> requests.forEach(request -> batchFacetsList(logicToDecorate, request)));
    return input -> {
      if (outputLogicExecutionInput == null) {
        outputLogicExecutionInput = input;
      }
      input
          .executionItems()
          .forEach(
              f -> {
                if (!(f.facetValues() instanceof BatchEnabledFacetValues)) {
                  throw new IllegalStateException(
                      "Expected to receive instance of BatchEnabledFacetValues in batcher for %s but received %s"
                          .formatted(context.vajramID(), f));
                }
              });
      List<BatchedFacets> batchedFacetsList = new ArrayList<>();
      for (ExecutionItem executionItem : input.executionItems()) {
        batchedFacetsList.addAll(inputBatcher.add(executionItem));
      }
      for (BatchedFacets batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
    };
  }

  private InputBatcher getInputBatcher(DependentChain dependentChain) {
    Integer epoch = epochByDepChain.get(dependentChain);
    if (epoch == null) {
      return simpleInputBatchersByDepChain.computeIfAbsent(
          dependentChain, _d -> inputBatcherFactory.get());
    } else {
      return sharedInputBatchersByEpoch.get(epoch);
    }
  }

  @Override
  public void flushDecorator(FlushCommand flushCommand) {
    DependentChain dependentChain = flushCommand.dependentChain();
    Integer epoch = epochByDepChain.get(dependentChain);
    Set<DependentChain> dependentChainsToFlush = Set.of();
    if (epoch != null) {
      dependentChainsToFlush = dependentChainsToFlushByEpoch.get(epoch);
      dependentChainsToFlush.remove(dependentChain);
    }
    if (dependentChainsToFlush.isEmpty()) {
      getInputBatcher(dependentChain).batch();
    }
  }

  @Override
  public void initiateActiveDepChains(InitiateActiveDepChains initiateActiveDepChains) {
    for (EpochGroup epochGroup : depChainsByEpoch.values()) {
      Set<DependentChain> dependentChains = epochGroup.dependentChains();
      dependentChainsToFlushByEpoch.add(
          new LinkedHashSet<>(
              // Retain only the ones which are applicable for this epoch
              Sets.intersection(dependentChains, initiateActiveDepChains.dependentChains())));
    }
  }

  @SuppressWarnings({"UnnecessaryTypeArgument", "unchecked"}) // --> To Handle nullChecker errors
  private void batchFacetsList(OutputLogic<Object> logicToDecorate, BatchedFacets batchedFacets) {
    List<ExecutionItem> facetsList = batchedFacets.batchItems();
    if (outputLogicExecutionInput == null) {
      if (facetsList.isEmpty()) {
        // This means the logicToDecorate (the output logic) method was never invoked
        // So we don't do anything
        return;
      } else {
        throw new AssertionError(
            "The decorateLogic was never invoked but facetsList is not empty. This should not be possible");
      }
    } else {
      try {
        logicToDecorate.execute(outputLogicExecutionInput.withExecutionItems(facetsList));
      } catch (Throwable e) {
        for (ExecutionItem f : facetsList) {
          f.response().completeExceptionally(wrapAsCompletionException(e));
        }
      }
    }
  }
}
