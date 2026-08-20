package com.flipkart.krystal.vajram.batching;

import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.data.ExecutionItem;
import com.flipkart.krystal.data.ImmutableFacetValues;
import com.flipkart.krystal.data.ImmutableFacetValuesContainer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An implementation of {@link InputBatcher} which measures batch size as the UNIQUE set of pending
 * unbatched facets objects. For example, even if multiple facet objects are equal to each other,
 * then they are considered equivalent to a batch size of 1. This is done so because a subsequent
 * caching decorator would deduplicate them anyway. If we don't count only unique facet value
 * objects while counting pending batch size, then we would end up create much smaller effective
 * batches because a subsequent CachingDecorator would dedupe most of them.
 */
@Slf4j
public final class InputBatcherImpl implements InputBatcher {

  private static final int DEFAULT_BATCH_SIZE = 10;
  private @Nullable Consumer<List<BatchedFacets>> batchingListener;
  private final Map<ImmutableFacetValuesContainer, UnbatchedItems> unBatchedRequests =
      new HashMap<>();
  private final int uniqueFacetsBatchSize;

  public InputBatcherImpl() {
    this(DEFAULT_BATCH_SIZE);
  }

  public InputBatcherImpl(int uniqueFacetsBatchSize) {
    this.uniqueFacetsBatchSize = uniqueFacetsBatchSize;
  }

  @Override
  public List<BatchedFacets> add(ExecutionItem batchEnabledFacets) {
    if (batchEnabledFacets.facetValues()
        instanceof BatchEnabledFacetValues batchEnabledFacetValues) {
      ImmutableFacetValuesContainer batchKey = batchEnabledFacetValues._batchKey();
      unBatchedRequests
          .computeIfAbsent(batchKey, k -> new UnbatchedItems())
          .add(batchEnabledFacets);
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
    UnbatchedItems batchItems = unBatchedRequests.getOrDefault(batchKey, new UnbatchedItems());
    if (force || batchItems.uniqueCount() >= uniqueFacetsBatchSize) {
      BatchedFacets batchedFacets = new BatchedFacets(batchItems.allExecutionItems());
      unBatchedRequests.put(batchKey, new UnbatchedItems());
      return List.of(batchedFacets);
    }
    return List.of();
  }

  @Override
  public void batch() {
    Consumer<List<BatchedFacets>> batchingListener = this.batchingListener;
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

  private record UnbatchedItems(
      List<ExecutionItem> allExecutionItems, Set<ImmutableFacetValues> uniqueFacetValues) {

    private UnbatchedItems() {
      this(new ArrayList<>(), new LinkedHashSet<>());
    }

    public void add(ExecutionItem executionItem) {
      allExecutionItems.add(executionItem);
      ImmutableFacetValues immut = getImmutableFacetValues(executionItem);
      if (immut != null) {
        // If immut is null, it means we are unable to build the instance as it might have some
        // missing mandatory fields for example. We don't have to add it to unique objects list
        // since that request will anyway fail and not contribute to the actual batch call
        uniqueFacetValues.add(immut);
      }
    }

    private static @Nullable ImmutableFacetValues getImmutableFacetValues(
        ExecutionItem executionItem) {
      ImmutableFacetValues immut;
      try {
        immut = executionItem.facetValues()._build();
      } catch (Exception e) {
        log.warn(
            "Unable to generate immutable value by 'building' facet values as an exception was encountered while building.",
            e);
        return null;
      }
      return immut;
    }

    public int uniqueCount() {
      return uniqueFacetValues.size();
    }
  }
}
