package com.flipkart.krystal.krystex.batching;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.krystex.epochs.EpochGroups;
import com.flipkart.krystal.krystex.epochs.VajramEpochGroups;
import com.flipkart.krystal.krystex.logicdecoration.LogicDecorationContext;
import com.flipkart.krystal.krystex.logicdecoration.OutputLogicDecorator;
import com.flipkart.krystal.vajram.batching.InputBatcherImpl;
import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record InputBatcherConfig(
    Function<LogicDecorationContext, @Nullable OutputLogicDecorator> decoratorFactory) {

  public static InputBatcherConfig computeDefaultBatcherConfig(
      EpochGroups epochGroups, BatchSizeSupplier batchSizeSupplier) {
    return new InputBatcherConfig(
        context -> {
          VajramID vajramID = context.vajramID();
          return new InputBatchingDecorator(
              () -> new InputBatcherImpl(batchSizeSupplier.getBatchSize(vajramID)),
              epochGroups
                  .vajramEpochGroups()
                  .getOrDefault(vajramID, new VajramEpochGroups(ImmutableMap.of())),
              context.activeDependentChains().get());
        });
  }
}
