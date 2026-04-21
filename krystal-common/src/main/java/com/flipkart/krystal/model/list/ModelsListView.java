package com.flipkart.krystal.model.list;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

public class ModelsListView<M extends Model, I extends ImmutableModel> extends AbstractList<I>
    implements RandomAccess, UnmodifiableImmutModelsList<M, I> {

  public static <M extends Model, I extends ImmutableModel> ModelsListView<M, I> empty() {
    return BasicModelsListBuilder.<M, I, ImmutableModel.Builder>empty().immutModelsView();
  }

  private final List<I> delegate;

  @NotOnlyInitialized private final ModelsListBuilder<M, I, ?> modelsBuilder;

  @SuppressWarnings("unchecked")
  ModelsListView(
      @UnderInitialization ModelsListBuilder<M, I, ?> modelsListBuilder, List<M> models) {
    this.modelsBuilder = modelsListBuilder;
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
  public <B extends ImmutableModel.Builder> ModelsListBuilder<M, I, B> modelsBuilder() {
    return (ModelsListBuilder<M, I, B>) modelsBuilder;
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
