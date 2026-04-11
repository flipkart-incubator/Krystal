package com.flipkart.krystal.model;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PACKAGE;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;
import lombok.Getter;

public class ModelsListView<M extends Model, I extends ImmutableModel> extends AbstractList<M>
    implements RandomAccess, UnmodifiableModelList<M, I> {

  private final List<M> delegate;

  @Getter(PACKAGE)
  private final ModelListBuilder<M, I, ?> source;

  public ModelsListView() {
    this(ModelListBuilder.empty());
  }

  @SuppressWarnings("unchecked")
  public ModelsListView(ModelListBuilder<M, I, ?> modelListBuilder) {
    this.source = modelListBuilder;
    this.delegate =
        Lists.transform(
            modelListBuilder.models(),
            model ->
                model instanceof ImmutableModel immut
                    ? (M) immut
                    : (M) requireNonNull(model)._asBuilder()._build());
  }

  @Override
  public M get(int index) {
    return delegate.get(index);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @SuppressWarnings("unchecked")
  public ModelsListView<I, I> asImmutableModelsList() {
    return (ModelsListView<I, I>) this;
  }
}
