package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.SimpleRequest;

public record GranuleResponse(Request<Object> facets, Errable<Object> response)
    implements KryonResponse {

  public GranuleResponse() {
    this(SimpleRequest.empty(), Errable.empty());
  }
}
