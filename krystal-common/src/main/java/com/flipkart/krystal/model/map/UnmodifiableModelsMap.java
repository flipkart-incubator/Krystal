package com.flipkart.krystal.model.map;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.Set;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UnmodifiableModelsMap<K, M extends Model, I extends ImmutableModel>
    extends AbstractMap<K, M> {

  @NotOnlyInitialized private final UnmodifiableImmutModelsMap<K, M, I> delegate;

  public UnmodifiableModelsMap(
      @UnknownInitialization UnmodifiableImmutModelsMap<K, M, I> delegate) {
    this.delegate = delegate;
  }

  public <B extends ImmutableModel.Builder> ModelsMapBuilder<K, M, I, B> modelsBuilder() {
    return delegate.modelsBuilder();
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable M get(Object key) {
    return (M) delegate.get(key);
  }

  @SuppressWarnings({"unchecked", "keyfor"})
  @Override
  public Set<Entry<K, M>> entrySet() {
    return Maps.transformValues(delegate, model -> (M) model).entrySet();
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
