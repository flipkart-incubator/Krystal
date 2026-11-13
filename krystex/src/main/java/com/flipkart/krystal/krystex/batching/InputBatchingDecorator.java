package com.flipkart.krystal.krystex.batching;

import static com.flipkart.krystal.except.KrystalCompletionException.wrapAsCompletionException;

import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.krystex.OutputLogic;
import com.flipkart.krystal.krystex.OutputLogicDefinition;
import com.flipkart.krystal.krystex.decoration.FlushCommand;
import com.flipkart.krystal.krystex.decoration.FlushableDecorator;
import com.flipkart.krystal.krystex.epochs.EpochGroup;
import com.flipkart.krystal.krystex.epochs.EpochGroups;
import com.flipkart.krystal.krystex.epochs.EpochGroupsByAncestors;
import com.flipkart.krystal.krystex.epochs.VajramEpochGroups;
import com.flipkart.krystal.krystex.kryon.DependentChain;
import com.flipkart.krystal.krystex.logicdecoration.LogicExecutionContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.BatchEnabledFacetValues;
import com.flipkart.krystal.vajram.batching.BatchedFacets;
import com.flipkart.krystal.vajram.batching.InputBatcher;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class InputBatchingDecorator implements OutputLogicDecorator, FlushableDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final List<InputBatcher> sharedInputBatchersByEpoch;
  private final Map<DependentChain, InputBatcher> simpleInputBatchersByDepChain =
      new LinkedHashMap<>();
  private final VajramID vajramID;
  private final Supplier<InputBatcher> inputBatcherFactory;
  private final EpochGroupsByAncestors epochGroupsByAncestors;
  private final ImmutableMap<DependentChain, Integer> epochByDepChains;
  private @MonotonicNonNull OutputLogicExecutionInput outputLogicExecutionInput;
  private final int[] yetToFlushCountsByEpoch;

  public InputBatchingDecorator(
      Supplier<InputBatcher> inputBatcherFactory,
      VajramID vajramID,
      EpochGroupsByAncestors epochGroupsByAncestors) {
    this.vajramID = vajramID;
    this.inputBatcherFactory = inputBatcherFactory;
    this.epochGroupsByAncestors = epochGroupsByAncestors;
    VajramEpochGroups vajramEpochGroups =
        epochGroupsByAncestors
            .allEpochGroups()
            .vajramEpochGroups()
            .getOrDefault(vajramID, new VajramEpochGroups(vajramID, ImmutableMap.of()));
    this.epochByDepChains = vajramEpochGroups.epochByDepChains();
    this.sharedInputBatchersByEpoch = new ArrayList<>(vajramEpochGroups.maxEpoch() + 1);
    this.yetToFlushCountsByEpoch = new int[vajramEpochGroups.maxEpoch() + 1];
    for (int epoch = 0; epoch <= vajramEpochGroups.maxEpoch(); epoch++) {
      this.sharedInputBatchersByEpoch.add(inputBatcherFactory.get());
      EpochGroup epochGroup = vajramEpochGroups.depChainsByEpochs().get(epoch);
      if (epochGroup == null) {
        continue;
      }
      this.yetToFlushCountsByEpoch[epoch] = epochGroup.dependentChains().size();
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
                      "Expected to receive instance of BatchEnabledFacetValues in batcher for %s but received %s in batching decorator of vajram %s"
                          .formatted(context.vajramID(), f, vajramID));
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
    Integer epoch = epochByDepChains.get(dependentChain);
    if (epoch == null) {
      return simpleInputBatchersByDepChain.computeIfAbsent(
          dependentChain, _d -> inputBatcherFactory.get());
    } else {
      return sharedInputBatchersByEpoch.get(epoch);
    }
  }

  @Override
  public void flushDecorator(FlushCommand flushCommand) {
    DependentChain ancestor = flushCommand.ancestor();
    VajramID fromVajramId = flushCommand.fromVajramId();
    InputBatcher simpleInputBatcher = simpleInputBatchersByDepChain.get(ancestor);
    if (simpleInputBatcher != null) {
      simpleInputBatcher.batch();
    }
    EpochGroups epochGroupsForAncestor =
        epochGroupsByAncestors
            .epochGroupsFromVajram()
            .getOrDefault(ancestor, ImmutableMap.of())
            .get(fromVajramId);
    if (epochGroupsForAncestor == null) {
      return;
    }
    VajramEpochGroups vajramEpochsForAncestor =
        epochGroupsForAncestor.vajramEpochGroups().get(vajramID);
    if (vajramEpochsForAncestor == null) {
      return;
    }
    ImmutableMap<Integer, EpochGroup> depChainsByEpochs =
        vajramEpochsForAncestor.depChainsByEpochs();
    depChainsByEpochs.forEach(
        (epoch, epochGroup) -> {
          int oldSize = yetToFlushCountsByEpoch[epoch];
          if (oldSize > 0) {
            yetToFlushCountsByEpoch[epoch] -= epochGroup.dependentChains().size();
            int newSize = yetToFlushCountsByEpoch[epoch];
            if (newSize <= 0) {
              sharedInputBatchersByEpoch.get(epoch).batch();
            }
          }
        });
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
            "The decorateLogic was never invoked but facetsList is not empty. This should not be possible. Vajram: %s"
                .formatted(vajramID));
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
