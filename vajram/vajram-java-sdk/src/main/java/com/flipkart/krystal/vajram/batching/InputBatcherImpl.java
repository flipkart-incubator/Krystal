package com.flipkart.krystal.vajram.batching;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.ImmutableFacetContainer;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatcherImpl implements InputBatcher {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private @Nullable Consumer<ImmutableList<BatchedFacets>> batchingListener;
  private final Map<ImmutableFacetContainer, List<BatchEnabledFacets>> unBatchedRequests =
      new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public InputBatcherImpl() {}

  public InputBatcherImpl(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<BatchedFacets> add(BatchEnabledFacets batchEnabledFacets) {
    ImmutableFacetContainer commonFacets = batchEnabledFacets._common();
    unBatchedRequests.computeIfAbsent(commonFacets, k -> new ArrayList<>()).add(batchEnabledFacets);
    return getBatchedInputs(commonFacets, false);
  }

  private ImmutableList<BatchedFacets> getBatchedInputs(
      ImmutableFacetContainer commonFacets, boolean force) {
    if (commonFacets == null) {
      return ImmutableList.of();
    }
    List<BatchEnabledFacets> batchItems =
        unBatchedRequests.getOrDefault(commonFacets, ImmutableList.of());
    if (force || batchItems.size() >= minBatchSize) {
      ImmutableList<BatchedFacets> batchedFacets =
          ImmutableList.of(new BatchedFacets(ImmutableList.copyOf(batchItems)));
      unBatchedRequests.put(commonFacets, new ArrayList<>());
      return batchedFacets;
    }
    return ImmutableList.of();
  }

  @Override
  public void batch() {
    ImmutableList<BatchedFacets> batchedFacets =
        unBatchedRequests.keySet().stream()
            .map(c -> getBatchedInputs(c, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    if (batchingListener != null) {
      batchingListener.accept(batchedFacets);
    }
  }

  @Override
  public void onBatching(Consumer<ImmutableList<BatchedFacets>> listener) {
    batchingListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
