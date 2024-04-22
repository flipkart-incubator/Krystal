package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public abstract non-sealed class RequestBuilder<T> implements Request<T>, FacetContainerBuilder {

  @Override
  public abstract RequestBuilder<T> _set(int facetId, FacetValue<?> value);

  @Override
  public final RequestBuilder<T> _asBuilder() {
    return this;
  }

  @Override
  public abstract RequestBuilder<T> _newCopy();
}
