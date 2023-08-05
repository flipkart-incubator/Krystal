package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.data.ValueOrError;

record GranuleResponse(Inputs inputs, ValueOrError<Object> response)
    implements KryonResponse {

  GranuleResponse() {
    this(Inputs.empty(), ValueOrError.empty());
  }
}
