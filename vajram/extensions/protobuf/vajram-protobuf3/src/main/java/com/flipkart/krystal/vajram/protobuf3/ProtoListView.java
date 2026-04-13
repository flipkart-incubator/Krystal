package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.ImmutableModelList;
import com.flipkart.krystal.model.Model;
import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

public final class ProtoListView<M extends Model, I extends ImmutableModel> extends AbstractList<I>
    implements RandomAccess, ImmutableModelList<M, I> {

  private final ProtoListBuilder<M, I, ?> builder;
  private final List<M> delegate;

  ProtoListView(ProtoListBuilder<M, I, ?> builder, List<M> delegate) {
    this.builder = builder;
    this.delegate = delegate;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends Builder> ProtoListBuilder<M, I, B> modelsBuilder() {
    return (ProtoListBuilder<M, I, B>) builder;
  }

  @SuppressWarnings("unchecked")
  @Override
  public I get(int index) {
    return (I) delegate.get(index)._build();
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
