package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public abstract non-sealed class ImmutableRequest<T> implements Request<T>, ImmutableModel {

  @Override
  public final ImmutableRequest<T> _build() {
    return this;
  }

  @Override
  public final Request<T> _newCopy() {
    return this;
  }

  @Override
  public abstract ImmutableMap<Integer, Errable<Object>> _asMap();
}
