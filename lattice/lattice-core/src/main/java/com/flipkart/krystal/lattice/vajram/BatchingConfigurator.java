package com.flipkart.krystal.lattice.vajram;

import com.flipkart.krystal.traits.TraitDispatchPolicies;
import com.flipkart.krystal.vajramexecutor.krystex.batching.InputBatcherConfig;

@FunctionalInterface
public interface BatchingConfigurator {
  InputBatcherConfig createBatcherConfig(BatchingConfiguratorContext context);

  record BatchingConfiguratorContext(TraitDispatchPolicies traitDispatchPolicies) {}
}
