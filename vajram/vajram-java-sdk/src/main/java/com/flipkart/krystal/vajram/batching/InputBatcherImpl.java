package com.flipkart.krystal.vajram.batching;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatcherImpl<B extends Facets, C extends Facets>
    implements InputBatcher<B, C> {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private @Nullable Consumer<ImmutableList<BatchedFacets<B, C>>> batchingListener;
  private final Map<C, List<B>> unBatchedRequests = new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public InputBatcherImpl() {}

  public InputBatcherImpl(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<BatchedFacets<B, C>> add(B batchableFacets, C commonFacets) {
    C immutableCommonFacets = makeImmutable(commonFacets);
    unBatchedRequests
        .computeIfAbsent(immutableCommonFacets, k -> new ArrayList<>())
        .add(batchableFacets);
    return getBatchedInputs(immutableCommonFacets, false);
  }

  private ImmutableList<BatchedFacets<B, C>> getBatchedInputs(C commonFacets, boolean force) {
    if (commonFacets == null) {
      return ImmutableList.of();
    }
    C immutableFacets = makeImmutable(commonFacets);
    List<B> batchableInputs = unBatchedRequests.getOrDefault(immutableFacets, ImmutableList.of());
    if (force || batchableInputs.size() >= minBatchSize) {
      unBatchedRequests.put(immutableFacets, new ArrayList<>());
      return ImmutableList.of(
          new BatchedFacets<>(ImmutableList.copyOf(batchableInputs), immutableFacets));
    }
    return ImmutableList.of();
  }

  @Override
  public void batch() {
    ImmutableList<BatchedFacets<B, C>> batchedFacets =
        unBatchedRequests.keySet().stream()
            .map(c -> getBatchedInputs(c, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    if (batchingListener != null) {
      batchingListener.accept(batchedFacets);
    }
  }

  @Override
  public void onBatching(Consumer<ImmutableList<BatchedFacets<B, C>>> listener) {
    batchingListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }

  private C makeImmutable(C commonFacets) {
    //noinspection unchecked
    return (C) commonFacets._build();
  }
}
