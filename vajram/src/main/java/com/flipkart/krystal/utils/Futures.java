package com.flipkart.krystal.utils;

import java.util.concurrent.CompletableFuture;

public class Futures {

  public static void propagateCancellation(CompletableFuture<?> from, CompletableFuture<?> to) {
    from.whenComplete(
        (unused, throwable) -> {
          if (from.isCancelled() && !to.isDone()) {
            to.cancel(true);
          }
        });
  }

  private Futures() {}
}
