package com.flipkart.krystal.vajram.batching;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.config.ConfigProvider;
import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class InputBatcherImpl implements InputBatcher {

  private static final int DEFAULT_BATCH_SIZE = 1;
  private @Nullable Consumer<List<BatchedFacets>> batchingListener;
  private final Map<ImmutableFacetValuesContainer, List<ExecutionItem>> unBatchedRequests =
      new HashMap<>();
  private int minBatchSize = DEFAULT_BATCH_SIZE;

  public InputBatcherImpl() {}

  public InputBatcherImpl(int minBatchSize) {
    this.minBatchSize = minBatchSize;
  }

  @Override
  public List<BatchedFacets> add(ExecutionItem batchEnabledFacets) {
    if (batchEnabledFacets.facetValues()
        instanceof BatchEnabledFacetValues batchEnabledFacetValues) {
      ImmutableFacetValuesContainer batchKey = batchEnabledFacetValues._batchKey();
      unBatchedRequests.computeIfAbsent(batchKey, k -> new ArrayList<>()).add(batchEnabledFacets);
      return getBatchedInputs(batchKey, false);
    } else {
      throw new IllegalStateException(
          "Expected to receive instance of BatchEnabledFacetValues in batcher but received %s for vajram %s"
              .formatted(
                  batchEnabledFacets.facetValues(), batchEnabledFacets.facetValues()._vajramID()));
    }
  }

  private List<BatchedFacets> getBatchedInputs(
      ImmutableFacetValuesContainer batchKey, boolean force) {
    List<ExecutionItem> batchItems = unBatchedRequests.getOrDefault(batchKey, ImmutableList.of());
    if (force || batchItems.size() >= minBatchSize) {
      BatchedFacets batchedFacets = new BatchedFacets(batchItems);
      unBatchedRequests.put(batchKey, new ArrayList<>());
      return List.of(batchedFacets);
    }
    return List.of();
  }

  @Override
  public void batch() {
    if (batchingListener != null) {
      List<BatchedFacets> list = new ArrayList<>();
      for (ImmutableFacetValuesContainer c : unBatchedRequests.keySet()) {
        list.addAll(getBatchedInputs(c, true));
      }
      batchingListener.accept(unmodifiableList(list));
    }
  }

  @Override
  public void onBatching(Consumer<List<BatchedFacets>> listener) {
    batchingListener = listener;
  }

  @Override
  public void onConfigUpdate(ConfigProvider configProvider) {
    this.minBatchSize =
        configProvider.<Integer>getConfig("min_batch_size").orElse(DEFAULT_BATCH_SIZE);
  }
}
