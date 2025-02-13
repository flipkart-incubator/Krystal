package com.flipkart.krystal.vajram.exec;

/**
 * A graph representation of a krystal graph for which executors multiple times and then executed.
 *
 * @param <EC> Configuration for creating and executor of this graph
 */
public interface VajramExecutableGraph<EC> extends AutoCloseable {
  VajramExecutor createExecutor(EC executorConfig);

  @Override
  void close();
}
