package com.flipkart.krystal.krystex.batching;

import com.flipkart.krystal.core.VajramID;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.Builder;
import lombok.Singular;

@Builder
public record InputBatcherConfig(
    @Singular
        ImmutableMap<VajramID, ImmutableList<DepChainBatcherConfig>> depChainBatcherConfigs) {}
