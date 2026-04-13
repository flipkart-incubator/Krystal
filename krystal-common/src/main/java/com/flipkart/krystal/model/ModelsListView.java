package com.flipkart.krystal.model;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public class ModelsListView<M extends Model, I extends ImmutableModel> extends AbstractList<I>
    implements RandomAccess, ImmutableModelList<M, I> {

  public static <M extends Model, I extends ImmutableModel> ModelsListView<M, I> empty() {
    return BasicModelListBuilder.<M, I, ImmutableModel.Builder>empty().immutModelsView();
  }

  private final List<I> delegate;

  private final ModelListBuilder<M, I, ?> modelsBuilder;

  @SuppressWarnings("unchecked")
  ModelsListView(ModelListBuilder<M, I, ?> modelListBuilder, List<M> models) {
    this.modelsBuilder = modelListBuilder;
    this.delegate =
        Lists.transform(
            models,
            model ->
                model instanceof ImmutableModel immut
                    ? (I) immut
                    : (I) requireNonNull(model)._asBuilder()._build());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends ImmutableModel.Builder> ModelListBuilder<M, I, B> modelsBuilder() {
    return (ModelListBuilder<M, I, B>) modelsBuilder;
  }

  @SuppressWarnings("unchecked")
  @Override
  public I get(int index) {
    return delegate.get(index);
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
