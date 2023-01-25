package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;

public record NodeResponse(Inputs inputs, ValueOrError<Object> response, boolean shouldTerminate) {

  public NodeResponse() {
    this(Inputs.empty(), ValueOrError.empty(), false);
  }
}
