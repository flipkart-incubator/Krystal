package com.flipkart.krystal.model.map;

import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.Maps;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

public class ModelsMapView<K, M extends Model, I extends ImmutableModel> extends AbstractMap<K, I>
    implements ImmutModelsMapView<K, M, I> {

  public static <K, M extends Model, I extends ImmutableModel> ModelsMapView<K, M, I> empty() {
    return BasicModelsMapBuilder.<K, M, I, ImmutableModel.Builder>empty().immutModelsView();
  }

  private final Map<K, I> delegate;

  private final ModelsMapBuilder<K, M, I, ?> modelsBuilder;

  @SuppressWarnings("unchecked")
  ModelsMapView(ModelsMapBuilder<K, M, I, ?> modelListBuilder, Map<K, M> models) {
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
  public I get(Object key) {
    return delegate.get(key);
  }

  @Override
  public Set<Entry<K, I>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
