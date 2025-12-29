package com.flipkart.krystal.vajramexecutor.krystex.batching;

import static com.flipkart.krystal.except.KrystalException.wrapAsCompletionException;
import static java.util.Collections.unmodifiableSet;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.config.NestedConfig;
import com.flipkart.krystal.core.OutputLogicExecutionInput;
import com.flipkart.krystal.data.ExecutionItem;
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
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class InputBatchingDecorator implements OutputLogicDecorator {

  public static final String DECORATOR_TYPE = InputBatchingDecorator.class.getName();

  private final String instanceId;
  private final InputBatcher inputBatcher;
  private final Predicate<DependentChain> isApplicableToDependentChain;
  private final Set<DependentChain> dependantChainsToFlush = new LinkedHashSet<>();
  private @MonotonicNonNull Set<DependentChain> activeDependantChains;
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
      input
          .facetValueResponses()
          .forEach(
              f -> {
                if (!(f.facetValues() instanceof BatchEnabledFacetValues)) {
                  throw new IllegalStateException(
                      "Expected to receive instance of BatchEnabledFacetValues in batcher %s but received %s"
                          .formatted(instanceId, f));
                }
              });
      List<BatchedFacets> batchedFacetsList = new ArrayList<>();
      for (ExecutionItem executionItem : input.facetValueResponses()) {
        batchedFacetsList.addAll(inputBatcher.add(executionItem));
      }
      for (BatchedFacets batchedFacets : batchedFacetsList) {
        batchFacetsList(logicToDecorate, batchedFacets);
      }
    };
  }

  @Override
  public void executeCommand(DecoratorCommand logicDecoratorCommand) {
    if (activeDependantChains == null
        && logicDecoratorCommand instanceof InitiateActiveDepChains initiateActiveDepChains) {
      Set<DependentChain> allActiveDepChains = initiateActiveDepChains.dependantsChains();
      Set<DependentChain> builder = new LinkedHashSet<>(allActiveDepChains.size());
      // Retain only the ones which are applicable for this input batching decorator
      for (DependentChain activeDepChain : allActiveDepChains) {
        if (isApplicableToDependentChain.test(activeDepChain)) {
          builder.add(activeDepChain);
        }
      }
      this.activeDependantChains = unmodifiableSet(builder);
      this.dependantChainsToFlush.addAll(this.activeDependantChains);
    } else if (logicDecoratorCommand instanceof FlushCommand flushCommand) {
      dependantChainsToFlush.remove(flushCommand.dependantsChain());
      if (dependantChainsToFlush.isEmpty()) {
        inputBatcher.batch();
        dependantChainsToFlush.addAll(activeDependantChains());
      }
    }
  }

  private Set<DependentChain> activeDependantChains() {
    return activeDependantChains == null ? ImmutableSet.of() : activeDependantChains;
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
        logicToDecorate.execute(outputLogicExecutionInput.withFacetValueResponses(facetsList));
      } catch (Throwable e) {
        for (ExecutionItem f : facetsList) {
          f.response().completeExceptionally(wrapAsCompletionException(e));
        }
      }
    }
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    inputBatcher.onConfigUpdate(
        new NestedConfig(String.format("input_batching.%s.", instanceId), configProvider));
  }
}
