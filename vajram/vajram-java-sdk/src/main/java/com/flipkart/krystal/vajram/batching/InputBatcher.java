package com.flipkart.krystal.vajram.batching;

import com.flipkart.krystal.config.ConfigListener;
import com.flipkart.krystal.data.ExecutionItem;
import java.util.List;
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
 */
public interface InputBatcher extends ConfigListener {

  List<BatchedFacets> add(ExecutionItem batchEnabledFacets);

  /** Externally trigger batching */
  void batch();

  /**
   * When this InputBatcher decides to batch (due to some internal state like a timer), or when the
   * {@link #batch()} method is called, execute the given callback.
   */
  void onBatching(Consumer<List<BatchedFacets>> callback);
}
