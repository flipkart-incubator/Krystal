package com.flipkart.krystal.krystex.batching;

import static com.flipkart.krystal.krystex.epochs.EpochGroups.computeEpochGroups;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.DependentChainDisabler;
import com.flipkart.krystal.krystex.VajramGraph;
import com.flipkart.krystal.krystex.epochs.EpochGroups;
import com.flipkart.krystal.krystex.epochs.VajramEpochGroups;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.function.Function;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record InputBatcherConfig(
    Function<VajramID, @Nullable OutputLogicDecorator> decoratorFactory) {

  public static InputBatcherConfig computeDefaultBatcherConfig(
      VajramGraph graph,
      BatchSizeSupplier batchSizeSupplier,
      TraitDispatchPolicies traitDispatchPolicies,
      DependentChainDisabler dependentChainDisabler,
      Collection<VajramID> externallyInvocableVajramIds) {
    EpochGroups epochGroups =
        computeEpochGroups(
            graph, traitDispatchPolicies, dependentChainDisabler, externallyInvocableVajramIds);
    return new InputBatcherConfig(
        vajramID ->
            new InputBatchingDecorator(
                () -> new InputBatcherImpl(batchSizeSupplier.getBatchSize(vajramID)),
                epochGroups
                    .vajramEpochGroups()
                    .getOrDefault(vajramID, new VajramEpochGroups(ImmutableMap.of()))));
  }
}
