package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;

public record GranuleResponse(Inputs inputs, ValueOrError<Object> response)
    implements KryonResponse {

  public GranuleResponse() {
    this(Inputs.empty(), ValueOrError.empty());
  }
}
