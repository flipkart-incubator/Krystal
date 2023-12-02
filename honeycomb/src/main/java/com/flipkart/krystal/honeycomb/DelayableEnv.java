package com.flipkart.krystal.honeycomb;

import com.flipkart.krystal.futures.DelayableFuture;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface DelayableEnv {
  <A, R> DelayableFuture<A, R> awaitDelayableResponse(CompletableFuture<A> acknowledgement);

  CallbackInfo getCallBackInfo();

  Function<Object, byte[]> byteSerializer();

  default Function<Object, String> stringSerializer() {
    return o -> Base64.getEncoder().encodeToString(byteSerializer().apply(o));
  }
}
