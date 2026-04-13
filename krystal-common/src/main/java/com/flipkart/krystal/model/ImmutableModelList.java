package com.flipkart.krystal.model;

import java.util.List;

public interface ImmutableModelList<M extends Model, I extends ImmutableModel> extends List<I> {
  <B extends ImmutableModel.Builder> ModelListBuilder<M, I, B> modelsBuilder();

  @SuppressWarnings("unchecked")
  default UnmodifiableModelList<M, I> asModelsView() {
    return new UnmodifiableModelList<>(this);
  }
}
