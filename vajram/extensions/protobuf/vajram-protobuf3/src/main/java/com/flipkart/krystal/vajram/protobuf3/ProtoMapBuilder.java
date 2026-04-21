package com.flipkart.krystal.vajram.protobuf3;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.model.map.UnmodifiableModelsMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ProtoMapBuilder<K, M extends Model, I extends ImmutableModel, B extends Builder>
    implements ModelsMapBuilder<K, M, I, B> {

  private final Supplier<Map<K, I>> modelsMap;
  private final BiFunction<K, @Nullable M, @Nullable M> getModelOrDefault;
  private final BiConsumer<K, I> putModel;
  private final BiConsumer<K, B> putBuilder;
  private final boolean builderExtendsModelRoot;
  private final Function<K, B> putBuilderIfAbsent;
  private final Consumer<Map<K, ? extends I>> putAllModels;
  private final Supplier<Integer> size;
  private final Predicate<K> contains;
  private final Runnable clear;
  private final Consumer<K> remove;

  public ProtoMapBuilder(
      Class<M> modelType,
      Class<I> immutModelType,
      Class<B> builderType,
      Supplier<Map<K, I>> modelsMap,
      Supplier<Integer> size,
      Predicate<K> contains,
      BiFunction<K, @Nullable M, @Nullable M> getModelOrDefault,
      Function<K, B> putBuilderIfAbsent,
      Consumer<Map<K, ? extends I>> putAllModels,
      BiConsumer<K, I> putModel,
      Runnable clear,
      Consumer<K> remove) {
    if (!modelType.isAssignableFrom(immutModelType)) {
      throw new IllegalArgumentException(
          "The Immutable Model type %s must be a subtype of the modelType %s"
              .formatted(immutModelType, modelType));
    }
    this.getModelOrDefault = getModelOrDefault;
    this.size = size;
    this.modelsMap = modelsMap;
    this.putBuilderIfAbsent = putBuilderIfAbsent;
    this.putAllModels = putAllModels;
    this.putModel = putModel;
    //noinspection unchecked
    this.putBuilder = (k, b) -> putModel.accept(k, (I) b._build());
    this.contains = contains;
    this.clear = clear;
    this.remove = remove;
    this.builderExtendsModelRoot = modelType.isAssignableFrom(builderType);
  }

  @Override
  public ProtoMapView<K, M, I> immutModelsView() {
    return new ProtoMapView<>(this, modelsMap);
  }

  @Override
  public UnmodifiableModelsMap<K, M, I> unmodifiableModelsView() {
    return new UnmodifiableModelsMap<>(immutModelsView());
  }

  @Override
  public int size() {
    return size.get();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(@NonNull K key) {
    return contains.test(key);
  }

  @Override
  public @Nullable M getModel(K key) {
    return getModelOrDefault.apply(key, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable B getBuilder(K key) {
    M value = getModelOrDefault.apply(key, null);
    if (value == null) {
      return null;
    }
    return (B) value._asBuilder();
  }

  @SuppressWarnings({"unchecked", "type.argument"})
  @Override
  public void putAllModels(Map<K, ? extends M> map) {
    putAllModels.accept(Maps.transformValues(map, m -> ((I) requireNonNull(m)._build())));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void putAllBuilders(Map<K, ? extends B> map) {
    if (builderExtendsModelRoot) {
      putAllModels((Map<K, ? extends M>) map);
      return;
    }
    for (Entry<K, ? extends B> entry : map.entrySet()) {
      putBuilder.accept(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    clear.run();
  }

  @SuppressWarnings("unchecked")
  @Override
  public M putModelIfAbsent(@NonNull K key, Supplier<I> valueSupplier) {
    M m = (M) modelsMap.get().get(key);
    if (m != null) {
      return m;
    }
    I valueToReplace = valueSupplier.get();
    putModel.accept(key, valueToReplace);
    return (M) valueToReplace;
  }

  @Override
  public B putBuilderIfAbsent(K key, Supplier<B> defaultValueSupplier) {
    boolean contains = this.contains.test(key);
    if (!contains) {
      B defaultValue = defaultValueSupplier.get();
      putBuilder(key, defaultValue);
    }
    // Get the build afresh so that the builder is a live version of the value in the map
    return putBuilderIfAbsent.apply(key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void putModel(K key, M value) {
    putModel.accept(key, (I) value._build());
  }

  /**
   * This will convert passed builder to an Immut and then put in the map because the put operation
   * in protobuf java only supports putting a proto, not a builder
   */
  @Override
  public void putBuilder(K key, B value) {
    putBuilder.accept(key, value);
  }

  @Override
  public @Nullable Model remove(@NonNull K key) {
    if (!containsKey(key)) {
      return null;
    }
    M model = getModel(key);
    remove.accept(key);
    return model;
  }
}
