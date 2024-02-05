package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;

public record GranuleResponse(Facets facets, Errable<Object> response) implements KryonResponse {

  public GranuleResponse() {
    this(Facets.empty(), Errable.empty());
  }
}
