package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ValueOrError;

public record GranuleResponse(Facets facets, ValueOrError<Object> response)
    implements KryonResponse {

  public GranuleResponse() {
    this(Facets.empty(), ValueOrError.empty());
  }
}
