package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface ImmutableRequest<T> extends Request<T>, ImmutableFacetContainer {

  @Override
  ImmutableRequest _build();

  @Override
  ImmutableRequest _newCopy();
}
