package com.flipkart.krystal.krystex.kryon;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.SimpleImmutRequest;

public record GranuleResponse(Request facets, Errable<Object> response)
    implements KryonResponse {

  public GranuleResponse() {
    this(SimpleImmutRequest.empty(), Errable.nil());
  }
}
