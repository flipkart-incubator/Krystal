package com.flipkart.krystal.vajram.exec;

/**
 * @param <EC> Configuration for creating and executor of this graph
 */
public interface VajramExecutableGraph<EC> extends AutoCloseable {
  VajramExecutor createExecutor(EC executorConfig);

  @Override
  void close();
}
