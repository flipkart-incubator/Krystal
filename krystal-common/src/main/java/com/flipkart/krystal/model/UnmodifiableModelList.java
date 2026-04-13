package com.flipkart.krystal.model;

import java.util.AbstractList;

public final class UnmodifiableModelList<M extends Model, I extends ImmutableModel>
    extends AbstractList<M> {

  private final ImmutableModelList<M, I> delegate;

  public UnmodifiableModelList(ImmutableModelList<M, I> delegate) {
    this.delegate = delegate;
  }

  public <B extends ImmutableModel.Builder> ModelListBuilder<M, I, B> modelsBuilder() {
    return delegate.modelsBuilder();
  }

  @SuppressWarnings("unchecked")
  @Override
  public M get(int index) {
    return (M) delegate.get(index);
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
