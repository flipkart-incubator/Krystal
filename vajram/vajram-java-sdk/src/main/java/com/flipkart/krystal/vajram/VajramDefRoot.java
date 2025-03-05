package com.flipkart.krystal.vajram;

import com.flipkart.krystal.data.ImmutableRequest;

public sealed interface VajramDefRoot<T> permits VajramDef, VajramTraitDef {
  ImmutableRequest.Builder<T> newRequestBuilder();
}
