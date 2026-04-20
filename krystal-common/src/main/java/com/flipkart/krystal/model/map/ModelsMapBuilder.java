package com.flipkart.krystal.model.map;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import java.util.Map;

public interface ModelsMapBuilder<
    K, M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {
  static <K, M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelsMapBuilder<K, M, I, B> empty() {
    return BasicModelsMapBuilder.empty();
  }

  ImmutModelsMapView<K, M, I> immutModelsView();

  UnmodifiableModelsMap<K, M, I> unmodifiableModelsView();

  int size();

  boolean isEmpty();

  void putAllModels(Map<K, ? extends M> map);

  void putAllBuilders(Map<K, ? extends B> map);

  void clear();

  M getModel(K key);

  B getBuilder(K key);

  void putModel(K key, M element);

  void putBuilder(K key, B element);

  Model remove(K key);
}
