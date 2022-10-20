package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public interface KrystalExecutor {
  <T> CompletableFuture<Result<T>> addNode(Node<T> node);

}
