package com.flipkart.krystal.facets;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.Request;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A client-side mirror of an input facet. In distributed krystal graphs where client vajram is in a
 * different process and server vajram is in a different process, models generated for client-side
 * consumption use this class to represent an input facet instead of the {@link Facet} class which
 * is used on the server side, so that any server-side runtime dependencies of the Facet class do
 * not pollute the runtime of the client and hence preserving server-client abstraction.
 */
public interface InputMirror extends BasicFacetInfo {
  @Nullable Object getFromRequest(Request request);

  void setToRequest(ImmutableRequest.Builder request, @Nullable Object value);
}
