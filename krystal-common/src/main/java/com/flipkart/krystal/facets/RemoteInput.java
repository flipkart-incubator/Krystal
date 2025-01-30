package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.data.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RemoteInput extends BasicFacetInfo {
  @Nullable Object getFromRequest(Request request);

  void setToRequest(Builder request, @Nullable Object value);
}
