package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface RequestBuilder<T> extends Request<T>, FacetContainerBuilder {

  @Override
  default RequestBuilder _asBuilder() {
    return this;
  }

  @Override
  RequestBuilder _newCopy();
}
