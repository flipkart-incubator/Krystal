package com.flipkart.krystal.data;

import com.google.common.collect.ImmutableMap;

/**
 * @param <T> The response type of the vajram corresponding to this request
 */
public interface ImmutableRequest<T> extends Request<T>, ImmutableModel {

  @Override
  ImmutableRequest<T> _build();

  @Override
  ImmutableRequest<T> _newCopy();

  @Override
  ImmutableMap<Integer, ? extends Errable<?>> _asMap();
}
