package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.config.ConfigListener;
import com.google.common.collect.ImmutableList;
import java.util.function.Consumer;

/**
 * An input batcher has the ability to squash (modulate) multiple inputs into a smaller set of
 * inputs/collection of inputs. Batching is generally needed when performance optimizations can be
 * achieved by squashing multiple inputs into a single requests. (For example when making I/O calls
 * like network calls/Database queries).
 *
 * <p>Input batcher work by collecting multiple sets of inputs into a collection and "modulate" them
 * by squashing/merging these when some condition is met. For example, {@link InputBatcherImpl}
 * keeps collecting inputs until a minimum batch size is reached.
 *
 * @param <BatchableInputs> Those inputs whose diffetent values can be batched into a single batch
 *     request.
 * @param <CommonFacets> Those inputs which do not vary within a batch request. Meaning, two
 *     requests with differing CommonInputs can never be batched into a single batch request.
 */
public interface InputBatcher extends ConfigListener {

  ImmutableList<BatchedFacets> add(BatchEnabledFacets batchEnabledFacets);

  /** Externally trigger batching */
  void batch();

  /**
   * When this InputBatcher decides to batch (due to some internal state like a timer), or when
   * the {@link #batch()} method is called, execute the given callback.
   */
  void onBatching(Consumer<ImmutableList<BatchedFacets>> callback);
}
