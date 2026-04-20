package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.map.ImmutModelsMapView;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ProtoMapView<K, M extends Model, I extends ImmutableModel>
    extends AbstractMap<K, I> implements ImmutModelsMapView<K, M, I> {

  private final ProtoMapBuilder<K, M, I, ?> builder;
  private final Supplier<Map<K, M>> delegate;

  ProtoMapView(ProtoMapBuilder<K, M, I, ?> builder, Supplier<Map<K, M>> delegate) {
    this.builder = builder;
    this.delegate = delegate;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <B extends Builder> ModelsMapBuilder<K, M, I, B> modelsBuilder() {
    return (ModelsMapBuilder<K, M, I, B>) builder;
  }

  @SuppressWarnings("unchecked")
  @Override
  public I get(Object key) {
    M model = delegate.get().get(key);
    if (model == null) {
      return null;
    }
    return (I) model._build();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<Entry<K, I>> entrySet() {
    return delegate.get().entrySet().stream()
        .map(e -> (Entry<K, I>) new SimpleImmutableEntry<>(e.getKey(), (I) e.getValue()._build()))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public int size() {
    return delegate.get().size();
  }
}
