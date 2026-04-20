package com.flipkart.krystal.model.map;

import static java.util.Collections.unmodifiableMap;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.Nullable;

final class BasicModelsMapBuilder<K, M extends Model, I extends ImmutableModel, B extends Builder>
    implements ModelsMapBuilder<K, M, I, B> {

  public static <K, M extends Model, I extends ImmutableModel, B extends Builder>
      BasicModelsMapBuilder<K, M, I, B> empty() {
    return new BasicModelsMapBuilder<>(ImmutableMap.of());
  }

  /**
   * A list which can contain either an immutable model or it's builder. Immutable models are
   * replaced with Builders on-demand when the value needs to be mutated. This strategy allows us to
   * prevent unnecessary object creation unless actually needed.
   */
  private final MapHolder<K, Model> models;

  private final ModelsMapView<K, M, I> immutModelsView;
  private final UnmodifiableModelsMap<K, M, I> unmodifiableModelsView;

  @SuppressWarnings("unchecked")
  private BasicModelsMapBuilder(Map<K, M> models) {
    MapHolder<K, M> mapHolder;
    if (models instanceof ImmutableList) {
      mapHolder = new MapHolder<>(models);
    } else {
      mapHolder = new MapHolder<>(new LinkedHashMap<>(models));
    }
    this.models = (MapHolder<K, Model>) mapHolder;
    this.immutModelsView = new ModelsMapView<>(this, unmodifiableMap(mapHolder));
    this.unmodifiableModelsView = new UnmodifiableModelsMap<>(immutModelsView);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ModelsMapView<K, M, I> immutModelsView() {
    return immutModelsView;
  }

  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableModelsMap<K, M, I> unmodifiableModelsView() {
    return unmodifiableModelsView;
  }

  @Override
  public int size() {
    return models.size();
  }

  @Override
  public boolean isEmpty() {
    return models.isEmpty();
  }

  @Override
  public void putAllModels(Map<K, ? extends M> map) {
    models.putAll(map);
  }

  @Override
  public void putAllBuilders(Map<K, ? extends B> map) {
    models.putAll(map);
  }

  @Override
  public void clear() {
    models.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public M getModel(K key) {
    Model model = models.get(key);
    if (model instanceof ImmutableModel) {
      return (M) model;
    } else {
      return (M) model._build();
    }
  }

  @Override
  public B getBuilder(K key) {
    return ensureBuilderForKey(key);
  }

  @Override
  public void putModel(K key, M element) {
    models.put(key, element);
  }

  @Override
  public void putBuilder(K key, B element) {
    models.put(key, element);
  }

  @Override
  public Model remove(K key) {
    return models.remove(key);
  }

  private void ensureMutable() {
    models.ensureMutable();
  }

  @SuppressWarnings("unchecked")
  private B ensureBuilderForKey(K key) {
    Model elementAtIndex = models.get(key);
    if (elementAtIndex instanceof ImmutableModel _immut) {
      ensureMutable();
      Builder builder = _immut._asBuilder();
      models.put(key, builder);
      return (B) builder;
    } else {
      return (B) elementAtIndex._asBuilder();
    }
  }

  /**
   * A holder for a list that can be mutable or immutable. An immutable list is swapped with a
   * mutable list only when the list needs ot be modified, thus preventing unnecessary memory
   * allocation.
   *
   * @param <T>
   */
  private static class MapHolder<K, T> implements Map<K, T> {
    @Getter @Setter private Map<K, T> delegate;

    private MapHolder(Map<K, T> delegate) {
      this.delegate = delegate;
    }

    private void ensureMutable() {
      if (delegate instanceof ImmutableMap<K, T>) {
        delegate(new LinkedHashMap<>(delegate));
      }
    }

    @Override
    public int size() {
      return delegate.size();
    }

    @Override
    public boolean isEmpty() {
      return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return delegate.containsValue(value);
    }

    @Override
    public T get(Object key) {
      return delegate.get(key);
    }

    @Override
    public @Nullable T put(K key, T value) {
      ensureMutable();
      return delegate.put(key, value);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public @Nullable T remove(Object key) {
      if (!delegate.containsKey(key)) {
        return null;
      }
      ensureMutable();
      return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends T> m) {
      if (m.isEmpty()) {
        return;
      }
      ensureMutable();
      delegate.putAll(m);
    }

    @Override
    public void clear() {
      if (!delegate.isEmpty()) {
        ensureMutable();
        delegate.clear();
      }
    }

    @Override
    public Set<K> keySet() {
      return delegate.keySet();
    }

    @Override
    public Collection<T> values() {
      return delegate.values();
    }

    @Override
    public Set<Entry<K, T>> entrySet() {
      return delegate.entrySet();
    }
  }
}
