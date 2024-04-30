package com.flipkart.krystal.vajram.batching;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableFacets;
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
  private @Nullable Consumer<ImmutableList<BatchedFacets<Facets, Facets>>> batchingListener;
  private final Map<ImmutableFacets, List<Facets>> unBatchedRequests = new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public InputBatcherImpl() {}

  public InputBatcherImpl(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public ImmutableList<BatchedFacets<Facets, Facets>> add(
      Facets batchableFacets, Facets commonFacets) {
    ImmutableFacets immutableCommonFacets = commonFacets._build();
    unBatchedRequests
        .computeIfAbsent(immutableCommonFacets, k -> new ArrayList<>())
        .add(batchableFacets);
    return getBatchedInputs(immutableCommonFacets, false);
  }

  private ImmutableList<BatchedFacets<Facets, Facets>> getBatchedInputs(
      ImmutableFacets commonFacets, boolean force) {
    if (commonFacets == null) {
      return ImmutableList.of();
    }
    ImmutableFacets immutableFacets = ((Facets) commonFacets)._build();
    List<Facets> batchableInputs =
        unBatchedRequests.getOrDefault(immutableFacets, ImmutableList.of());
    if (force || batchableInputs.size() >= minBatchSize) {
      unBatchedRequests.put(immutableFacets, new ArrayList<>());
      return ImmutableList.of(
          new BatchedFacets<>(ImmutableList.copyOf(batchableInputs), immutableFacets));
    }
    return ImmutableList.of();
  }

  @Override
  public void batch() {
    ImmutableList<BatchedFacets<Facets, Facets>> batchedFacets =
        unBatchedRequests.keySet().stream()
            .map(c -> getBatchedInputs(c, true))
            .flatMap(Collection::stream)
            .collect(toImmutableList());
    if (batchingListener != null) {
      batchingListener.accept(batchedFacets);
    }
  }

  @Override
  public void onBatching(Consumer<ImmutableList<BatchedFacets<Facets, Facets>>> listener) {
    batchingListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
