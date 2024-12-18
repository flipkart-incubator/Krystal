package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface RequestBuilder<T> extends Request<T>, FacetContainerBuilder {

  @Override
  default RequestBuilder<T> _set(int facetId, FacetValue value){
    return this;
  }

  @Override
  RequestBuilder<T> _asBuilder();

  @Override
  RequestBuilder<T> _newCopy();
}
