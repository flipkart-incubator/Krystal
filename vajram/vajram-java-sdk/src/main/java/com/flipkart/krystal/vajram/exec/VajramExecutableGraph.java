package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.ApplicationRequestContext;

public interface VajramExecutableGraph extends AutoCloseable {
  <C extends ApplicationRequestContext> VajramExecutor<C> createExecutor(C requestContext);

  @Override
  void close();
}
