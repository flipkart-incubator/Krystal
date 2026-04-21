package com.flipkart.krystal.model.map;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ModelsMapView<K, M extends Model, I extends ImmutableModel>
    extends AbstractMap<K, I> implements UnmodifiableImmutModelsMap<K, M, I> {

  public static <K, M extends Model, I extends ImmutableModel> ModelsMapView<K, M, I> empty() {
    return BasicModelsMapBuilder.<K, M, I, ImmutableModel.Builder>empty().immutModelsView();
  }

  private final Map<K, I> delegate;

  @NotOnlyInitialized private final ModelsMapBuilder<K, M, I, ?> modelsBuilder;

  @SuppressWarnings("unchecked")
  ModelsMapView(
      @UnderInitialization ModelsMapBuilder<K, M, I, ?> modelListBuilder, Map<K, M> models) {
    this.modelsBuilder = modelListBuilder;
    this.delegate =
        Maps.transformValues(
            models,
            model ->
                model instanceof ImmutableModel immut
                    ? (I) immut
                    : (I) requireNonNull(model)._asBuilder()._build());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <B extends ImmutableModel.Builder> ModelsMapBuilder<K, M, I, B> modelsBuilder() {
    return (ModelsMapBuilder<K, M, I, B>) modelsBuilder;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable I get(Object key) {
    return delegate.get(key);
  }

  @SuppressWarnings("keyfor")
  @Override
  public Set<Entry<K, I>> entrySet() {
    return unmodifiableSet(delegate.entrySet());
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
