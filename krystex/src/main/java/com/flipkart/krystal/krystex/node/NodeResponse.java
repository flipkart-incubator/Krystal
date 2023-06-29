package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.krystex.request.RequestId;

public record NodeResponse(Inputs inputs, ValueOrError<Object> response, RequestId requestId) {

  public NodeResponse(RequestId requestId) {
    this(Inputs.empty(), ValueOrError.empty(), requestId);
  }
}
