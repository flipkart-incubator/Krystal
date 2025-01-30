package com.flipkart.krystal.data;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface ImmutableRequest<T> extends Request<T>, ImmutableFacetContainer {

  @Override
  default ImmutableRequest _build() {
    return this;
  }

  @Override
  ImmutableRequest _newCopy();

  /**
   * @param <T> The response type of the vajram corresponding to this request
   */
  interface Builder<T> extends Request<T>, FacetContainerBuilder {

    @Override
    default Builder<T> _asBuilder() {
      return this;
    }

    @Override
    Builder _newCopy();
  }
}
