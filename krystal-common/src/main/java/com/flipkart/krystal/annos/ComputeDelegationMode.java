package com.flipkart.krystal.annos;

import java.util.concurrent.CompletableFuture;

public enum ComputeDelegationMode {

  /**
   * The output logic of the vajram is executed in the same thread as the caller. Any vajram making
   * an IO call will not have this value.
   */
  NONE,

  /**
   * The output logic of the vajram in a separate thread/threadpool/core/process/microservice etc.
   * These are scenarios where the vajram needs to use resources outside the current thread's core
   * or heap memory to finish the computation.
   *
   * <p>The output logic returns a {@link CompletableFuture} which completes when the result of the
   * delegated computation is ready
   */
  SYNC,

  // Coming soon
  /* ASYNC, */
}
