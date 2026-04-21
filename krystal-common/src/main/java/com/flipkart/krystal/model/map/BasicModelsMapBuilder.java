package com.flipkart.krystal.model.map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

final class BasicModelsMapBuilder<K, M extends Model, I extends ImmutableModel, B extends Builder>
    implements ModelsMapBuilder<K, M, I, B> {

  public static <K, M extends Model, I extends ImmutableModel, B extends Builder>
      BasicModelsMapBuilder<K, M, I, B> empty() {
    return new BasicModelsMapBuilder<>(ImmutableMap.of());
  }

  /**
   * A map which can contain values each of which is either an immutable model or it's builder.
   * Immutable models are replaced with Builders on-demand when the value needs to be mutated. This
   * strategy allows us to prevent unnecessary object creation unless actually needed.
   */
  private final MapHolder<K, Model> models;

  @NotOnlyInitialized private final ModelsMapView<K, M, I> immutModelsView;
  @NotOnlyInitialized private final UnmodifiableModelsMap<K, M, I> unmodifiableModelsView;

  @SuppressWarnings({"unchecked", "initialization"})
  private BasicModelsMapBuilder(Map<K, M> models) {
    MapHolder<K, M> mapHolder;
    if (models instanceof ImmutableMap<K, M>) {
      mapHolder = new MapHolder<>(models);
    } else {
      mapHolder = new MapHolder<>(new LinkedHashMap<>(models));
    }
    this.models = (MapHolder<K, Model>) mapHolder;
    ModelsMapView<K, M, I> immutModelsView = new ModelsMapView<>(this, unmodifiableMap(mapHolder));
    this.immutModelsView = immutModelsView;
    this.unmodifiableModelsView = new UnmodifiableModelsMap<>(this.immutModelsView);
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

  @SuppressWarnings("RedundantCast") // For checkerframework
  @Override
  public boolean containsKey(@NonNull K key) {
    return models.containsKey((@KeyFor("this.models") K) key);
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable M getModel(@NonNull K key) {
    Model existingValue = models.get(key);
    if (existingValue == null) {
      return null;
    }
    return (M) existingValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public @Nullable B getBuilder(@NonNull K key) {
    Model existingValue = models.get(key);
    if (existingValue == null) {
      return null;
    }
    return (B) existingValue._asBuilder();
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

  @SuppressWarnings({
    "unchecked",
    // For CheckerFramework
    "RedundantCast"
  })
  @Override
  public M putModelIfAbsent(@NonNull K key, Supplier<I> defaultValueSupplier) {
    Model existingValue = models.get((@KeyFor("this.models") K) key);
    if (existingValue instanceof ImmutableModel) {
      return (M) existingValue;
    } else if (existingValue != null) {
      return (M) existingValue._build();
    } else {
      ensureMutable();
      I value = defaultValueSupplier.get();
      models.put((@KeyFor("this.models") K) key, value);
      return (M) value;
    }
  }

  @Override
  public B putBuilderIfAbsent(@NonNull K key, Supplier<B> defaultValueSupplier) {
    return ensureBuilderForKey(key, defaultValueSupplier);
  }

  @SuppressWarnings("RedundantCast") // For CheckerFramework
  @Override
  public void putModel(@NonNull K key, M value) {
    models.put((@KeyFor("this.models") K) key, value);
  }

  @SuppressWarnings("RedundantCast") // For CheckerFramework
  @Override
  public void putBuilder(@NonNull K key, B value) {
    models.put((@KeyFor("this.models") K) key, value);
  }

  @Override
  public @Nullable Model remove(@NonNull K key) {
    return models.remove(key);
  }

  private void ensureMutable() {
    models.ensureMutable();
  }

  @SuppressWarnings({
    "unchecked",
    // For CheckerFramework
    "RedundantCast"
  })
  private B ensureBuilderForKey(@NonNull K key, Supplier<? extends Model> defaultValueSupplier) {
    Model existingValue = models.get((@KeyFor("this.models") K) key);
    if (existingValue instanceof ImmutableModel _immut) {
      ensureMutable();
      Builder builder = _immut._asBuilder();
      models.put((@KeyFor("this.models") K) key, builder);
      return (B) builder;
    } else if (existingValue != null) {
      return (B) existingValue._asBuilder();
    } else {
      ensureMutable();
      Model defaultValue = defaultValueSupplier.get();
      B returnValue = (B) defaultValue._asBuilder();
      models.put((@KeyFor("this.models") K) key, returnValue);
      return returnValue;
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

    @SuppressWarnings("contracts.conditional.postcondition") // For CheckerFramework
    @Override
    public boolean containsKey(@NonNull Object key) {
      return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return delegate.containsValue(value);
    }

    @SuppressWarnings("return")
    @Override
    public T get(@NonNull Object key) {
      return delegate.get(key);
    }

    @SuppressWarnings({"RedundantCast", "contracts.postcondition"}) // For checker framework
    @Override
    public @Nullable T put(@Nullable K key, T value) {
      ensureMutable();
      return delegate.put((@KeyFor("this.delegate") K) requireNonNull(key), value);
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

    @SuppressWarnings("RedundantCast") // For NullChecker
    @Override
    public Set<@KeyFor("this") K> keySet() {
      return (Set<@KeyFor("this") K>) delegate.keySet();
    }

    @Override
    public Collection<T> values() {
      return delegate.values();
    }

    @Override
    @SuppressWarnings("keyfor")
    public Set<Entry<K, T>> entrySet() {
      return delegate.entrySet();
    }
  }
}
