package com.flipkart.krystal.graphql.test.productpage;

import com.flipkart.krystal.vajram.ApplicationRequestContext;

public record ProductPageRequestContext(
    String primaryProductId, String preferredListingId, String requestId)
    implements ApplicationRequestContext {

  @Override
  public String requestId() {
    return this.requestId;
  }
}
