package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.map.ModelsMapBuilder;
import com.flipkart.krystal.model.map.UnmodifiableImmutModelsMap;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ProtoMapView<K, M extends Model, I extends ImmutableModel>
    extends AbstractMap<K, I> implements UnmodifiableImmutModelsMap<K, M, I> {

  private final ProtoMapBuilder<K, M, I, ?> builder;
  private final Supplier<Map<K, I>> delegate;

  ProtoMapView(ProtoMapBuilder<K, M, I, ?> builder, Supplier<Map<K, I>> delegate) {
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
  public @Nullable I get(Object key) {
    M model = (M) delegate.get().get(key);
    if (model == null) {
      return null;
    }
    return (I) model._build();
  }

  @SuppressWarnings({
    "unchecked",
    // For CheckerFramework
    "RedundantCast"
  })
  @Override
  public Set<Entry<@KeyFor("this") K, I>> entrySet() {
    return (Set<Entry<@KeyFor("this") K, I>>) delegate.get().entrySet();
  }

  @Override
  public int size() {
    return delegate.get().size();
  }
}
