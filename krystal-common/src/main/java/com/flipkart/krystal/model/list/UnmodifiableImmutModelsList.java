package com.flipkart.krystal.model.list;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import java.util.List;

public interface UnmodifiableImmutModelsList<M extends Model, I extends ImmutableModel>
    extends List<I> {
  <B extends ImmutableModel.Builder> ModelsListBuilder<M, I, B> modelsBuilder();

  @SuppressWarnings("unchecked")
  default UnmodifiableModelsList<M, I> asModelsView() {
    return new UnmodifiableModelsList<>(this);
  }
}
