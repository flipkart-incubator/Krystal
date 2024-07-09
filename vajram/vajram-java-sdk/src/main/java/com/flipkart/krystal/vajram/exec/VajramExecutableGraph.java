package com.flipkart.krystal.vajram.exec;

public interface VajramExecutableGraph extends AutoCloseable {
  VajramExecutor createExecutor();

  @Override
  void close();
}
