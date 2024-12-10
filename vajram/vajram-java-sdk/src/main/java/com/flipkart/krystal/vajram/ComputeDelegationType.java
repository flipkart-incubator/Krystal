package com.flipkart.krystal.vajram;

import java.util.concurrent.CompletableFuture;

public enum ComputeDelegationType {

  /** Auto-Infer from code */
  DEFAULT,

  /**
   * The output logic of the vajram is executed in the same thread as the caller. These vajrams
   * extend from the {@link ComputeVajram} class.
   */
  NO_DELEGATION,

  /**
   * The output logic of the vajram in a seperate thread/thredpool/core/microservice etc. These are
   * scenarios where the vajram needs to use resources outside the current thread's core and heap
   * memory to finish the computation. These vajrams inherit from the {@link IOVajram} class
   *
   * <p>The output logic returns a {@link CompletableFuture} which completes when the result of the
   * delegated computation is ready
   */
  SYNC_DELEGATION,

  /*
  //Coming soon
  ASYNC_DELAGATION*/
}
