package com.flipkart.krystal.model.map;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ModelsMapBuilder<
    K, M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {
  static <K, M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelsMapBuilder<K, M, I, B> empty() {
    return BasicModelsMapBuilder.empty();
  }

  UnmodifiableImmutModelsMap<K, M, I> immutModelsView();

  UnmodifiableModelsMap<K, M, I> unmodifiableModelsView();

  int size();

  boolean isEmpty();

  boolean containsKey(@NonNull K key);

  @Nullable M getModel(@NonNull K key);

  @Nullable B getBuilder(@NonNull K key);

  M putModelIfAbsent(@NonNull K key, Supplier<I> modelSupplier);

  B putBuilderIfAbsent(@NonNull K key, Supplier<B> defaultValueSupplier);

  void putAllModels(Map<K, ? extends M> map);

  void putAllBuilders(Map<K, ? extends B> map);

  void clear();

  void putModel(@NonNull K key, M value);

  void putBuilder(@NonNull K key, B value);

  @Nullable Model remove(@NonNull K key);
}
