package com.flipkart.krystal.krystex;

import java.util.concurrent.CompletableFuture;

public record Result<T>(CompletableFuture<T> future) {}
