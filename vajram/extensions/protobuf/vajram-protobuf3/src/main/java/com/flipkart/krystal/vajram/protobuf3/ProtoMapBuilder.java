package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.model.map.UnmodifiableModelsMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProtoMapBuilder<
        K, M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
    implements ModelsMapBuilder<K, M, I, B> {

  private final Supplier<Map<K, M>> modelsDelegate;
  private final Supplier<Map<K, B>> buildersDelegate;
  private final BiConsumer<K, M> putModel;
  private final BiConsumer<K, B> putBuilder;
  private final Runnable clear;
  private final Function<K, Model> remove;
  private final boolean builderExtendsModelRoot;

  public ProtoMapBuilder(
      Class<M> modelType,
      Class<I> immutModelType,
      Class<B> builderType,
      Supplier<Map<K, M>> modelsDelegate,
      Supplier<Map<K, B>> buildersDelegate,
      BiConsumer<K, M> putModel,
      BiConsumer<K, B> putBuilder,
      Runnable clear,
      Function<K, Model> remove) {
    if (!modelType.isAssignableFrom(immutModelType)) {
      throw new IllegalArgumentException(
          "The Immutable Model type %s must be a subtype of the modelType %s"
              .formatted(immutModelType, modelType));
    }
    this.modelsDelegate = modelsDelegate;
    this.buildersDelegate = buildersDelegate;
    this.putModel = putModel;
    this.putBuilder = putBuilder;
    this.clear = clear;
    this.remove = remove;
    this.builderExtendsModelRoot = modelType.isAssignableFrom(builderType);
  }

  @Override
  public ProtoMapView<K, M, I> immutModelsView() {
    return new ProtoMapView<>(this, modelsDelegate);
  }

  @Override
  public UnmodifiableModelsMap<K, M, I> unmodifiableModelsView() {
    return new UnmodifiableModelsMap<>(immutModelsView());
  }

  @Override
  public int size() {
    return modelsDelegate.get().size();
  }

  @Override
  public boolean isEmpty() {
    return modelsDelegate.get().isEmpty();
  }

  @Override
  public void putAllModels(Map<K, ? extends M> map) {
    for (Map.Entry<K, ? extends M> entry : map.entrySet()) {
      putModel.accept(entry.getKey(), entry.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void putAllBuilders(Map<K, ? extends B> map) {
    if (builderExtendsModelRoot) {
      putAllModels((Map<K, ? extends M>) map);
      return;
    }
    for (Map.Entry<K, ? extends B> entry : map.entrySet()) {
      putBuilder.accept(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    clear.run();
  }

  @Override
  public M getModel(K key) {
    return modelsDelegate.get().get(key);
  }

  @Override
  public B getBuilder(K key) {
    return buildersDelegate.get().get(key);
  }

  @Override
  public void putModel(K key, M element) {
    putModel.accept(key, element);
  }

  @Override
  public void putBuilder(K key, B element) {
    putBuilder.accept(key, element);
  }

  @Override
  public Model remove(K key) {
    return remove.apply(key);
  }
}
