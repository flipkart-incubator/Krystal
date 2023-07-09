package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;

public record GranularNodeResponse(Inputs inputs, ValueOrError<Object> response)
    implements NodeResponse {

  public GranularNodeResponse() {
    this(Inputs.empty(), ValueOrError.empty());
  }
}
