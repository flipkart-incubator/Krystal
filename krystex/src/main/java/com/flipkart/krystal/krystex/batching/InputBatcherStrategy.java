package com.flipkart.krystal.krystex.batching;

import com.flipkart.krystal.krystex.KrystexGraph;

public sealed interface InputBatcherStrategy {

  /**
   * This indicates that the {@link KrystexGraph} should use the default shared batcher config
   * computed by {@link DepChainBatcherConfig}.
   *
   * @param batchSizeSupplier the batch size supplier to use to determine batch size for vajrams
   */
  record DefaultBatcherStrategy(BatchSizeSupplier batchSizeSupplier)
      implements InputBatcherStrategy {}

  /** Indicate that a custom batcher config is to be used by the {@link KrystexGraph} */
  record CustomBatcherStrategy(InputBatcherConfig customBatcherConfig)
      implements InputBatcherStrategy {}
}
