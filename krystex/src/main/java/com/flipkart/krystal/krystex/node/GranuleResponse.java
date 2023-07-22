package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;

public record GranuleResponse(Inputs inputs, ValueOrError<Object> response)
    implements NodeResponse {

  public GranuleResponse() {
    this(Inputs.empty(), ValueOrError.empty());
  }
}
