package com.flipkart.krystal.model.list;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import java.util.AbstractList;

public final class UnmodifiableModelsList<M extends Model, I extends ImmutableModel>
    extends AbstractList<M> {

  private final ImmutModelsListView<M, I> delegate;

  public UnmodifiableModelsList(ImmutModelsListView<M, I> delegate) {
    this.delegate = delegate;
  }

  public <B extends ImmutableModel.Builder> ModelsListBuilder<M, I, B> modelsBuilder() {
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
