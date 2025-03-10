package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;

public sealed interface VajramDefRoot<T> permits VajramDef, TraitDef {
  ImmutableRequest.Builder<T> newRequestBuilder();

  Class<? extends Request<T>> requestRoot();
}
