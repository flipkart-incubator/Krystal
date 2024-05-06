package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface RequestBuilder<T> extends Request<T>, FacetContainerBuilder {

  @Override
  RequestBuilder<T> _set(int facetId, FacetValue<?> value);

  @Override
  RequestBuilder<T> _asBuilder();

  @Override
  RequestBuilder<T> _newCopy();
}
