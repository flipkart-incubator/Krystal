package com.flipkart.krystal.model.map;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import java.util.Map;

public interface ImmutModelsMapView<K, M extends Model, I extends ImmutableModel>
    extends Map<K, I> {
  <B extends ImmutableModel.Builder> ModelsMapBuilder<K, M, I, B> modelsBuilder();

  @SuppressWarnings("unchecked")
  default UnmodifiableModelsMap<K, M, I> asModelsView() {
    return new UnmodifiableModelsMap<>(this);
  }
}
