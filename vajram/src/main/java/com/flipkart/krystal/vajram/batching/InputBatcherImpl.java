package com.flipkart.krystal.vajram.batching;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatcherImpl<I, C> implements InputBatcher<I, C> {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private @Nullable Consumer<ImmutableList<BatchedFacets<I, C>>> batchingListener;
  private final Map<C, List<I>> unBatchedRequests = new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public InputBatcherImpl() {}

  public InputBatcherImpl(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<BatchedFacets<I, C>> add(I batchableInputs, C commonFacets) {
    unBatchedRequests.computeIfAbsent(commonFacets, k -> new ArrayList<>()).add(batchableInputs);
    return getBatchedInputs(commonFacets, false);
  }

  private ImmutableList<BatchedFacets<I, C>> getBatchedInputs(C commonFacets, boolean force) {
    if (commonFacets == null) {
      return ImmutableList.of();
    }
    ImmutableList<I> batchableInputs =
        ImmutableList.copyOf(unBatchedRequests.getOrDefault(commonFacets, ImmutableList.of()));
    if (force || batchableInputs.size() >= minBatchSize) {
      unBatchedRequests.put(commonFacets, new ArrayList<>());
      return ImmutableList.of(new BatchedFacets<>(batchableInputs, commonFacets));
    }
    return ImmutableList.of();
  }

  @Override
  public void batch() {
    ImmutableList<BatchedFacets<I, C>> batchedFacets =
        unBatchedRequests.keySet().stream()
            .map(c -> getBatchedInputs(c, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    if (batchingListener != null) {
      batchingListener.accept(batchedFacets);
    }
  }

  @Override
  public void onBatching(Consumer<ImmutableList<BatchedFacets<I, C>>> listener) {
    batchingListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
