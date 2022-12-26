package com.flipkart.krystal.vajram.exec;

import com.flipkart.krystal.vajram.ApplicationRequestContext;

public interface VajramExecutableGraph {
  <C extends ApplicationRequestContext> VajramExecutor<C> createExecutor(C requestContext);
}
