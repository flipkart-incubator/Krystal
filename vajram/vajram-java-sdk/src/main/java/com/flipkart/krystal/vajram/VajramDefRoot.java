package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;

public sealed interface VajramDefRoot<T> permits VajramDef, TraitRoot {
  ImmutableRequest.Builder<T> newRequestBuilder();

  Class<? extends Request<T>> requestRootType();
}
