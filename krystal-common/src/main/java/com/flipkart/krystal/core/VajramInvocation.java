package com.flipkart.krystal.core;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestResponseFuture;
import java.util.List;

public interface VajramInvocation<T> {
  void executeVajram(RequestResponseFuture<Request<T>, T> requestResponseFuture);
}
