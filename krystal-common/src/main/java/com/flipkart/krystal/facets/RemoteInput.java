package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.data.RequestBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface RemoteInput extends BasicFacetInfo {
  @Nullable Object getFromRequest(Request request);

  void setToRequest(RequestBuilder request, @Nullable Object value);
}
