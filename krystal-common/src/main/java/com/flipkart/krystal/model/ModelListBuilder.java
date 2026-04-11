package com.flipkart.krystal.model;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PACKAGE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;

public final class ModelListBuilder<
    M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {

  public static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelListBuilder<M, I, B> empty() {
    return new ModelListBuilder<>(ImmutableList.of());
  }

  @SuppressWarnings("unchecked")
  public static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelListBuilder<M, I, B> ofModels(ModelsListView<M, I> modelsListView) {
    return (ModelListBuilder<M, I, B>) modelsListView.source();
  }

  @Getter(PACKAGE)
  private final ListHolder<Model> models;

  @SuppressWarnings("unchecked")
  private ModelListBuilder(List<M> models) {
    if (models instanceof ImmutableList) {
      this.models = new ListHolder<>((List<Model>) models);
    } else {
      this.models = new ListHolder<>(new ArrayList<>(models));
    }
  }

  public int size() {
    return models().size();
  }

  public boolean isEmpty() {
    return models().isEmpty();
  }

  @SuppressWarnings("unchecked")
  public boolean addModel(M m) {
    ensureMutable();
    return models().add(m);
  }

  public void addModel(int index, M element) {
    ensureMutable();
    models().add(index, element);
  }

  @SuppressWarnings("unchecked")
  public boolean addBuilder(B b) {
    ensureMutable();
    return models().add(b);
  }

  public void addBuilder(int index, B element) {
    ensureMutable();
    models().add(index, element);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllModels(Collection<? extends M> c) {
    ensureMutable();
    return models().addAll(c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllModels(int index, Collection<? extends M> c) {
    ensureMutable();
    return models().addAll(index, c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllBuilders(Collection<? extends B> c) {
    ensureMutable();
    return models().addAll(c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllBuilders(int index, Collection<? extends B> c) {
    ensureMutable();
    return models().addAll(index, c);
  }

  public void clear() {
    ensureMutable();
    models().clear();
  }

  public B get(int index) {
    return ensureBuilderAtIndex(index);
  }

  @SuppressWarnings("unchecked")
  public void setModel(int index, M element) {
    ensureMutable();
    models().set(index, element);
  }

  @SuppressWarnings("unchecked")
  public void setBuilder(int index, B element) {
    ensureMutable();
    models().set(index, element);
  }

  public Model remove(int index) {
    ensureMutable();
    return models().remove(index);
  }

  @SuppressWarnings("unchecked")
  public Iterator<B> iterator() {
    for (int i = 0; i < models().size(); i++) {
      ensureBuilderAtIndex(i);
    }
    return Iterators.transform(
        models().iterator(), model -> (B) requireNonNull(model)._asBuilder());
  }

  public ModelsListView<M, I> modelsListView() {
    return new ModelsListView<>(this);
  }

  private void ensureMutable() {
    models.ensureMutable();
  }

  @SuppressWarnings("unchecked")
  private B ensureBuilderAtIndex(int i) {
    Model elementAtIndex = models().get(i);
    if (elementAtIndex instanceof ImmutableModel _immut) {
      ImmutableModel.Builder builder = _immut._asBuilder();
      models().set(i, builder);
      return (B) builder;
    } else {
      return (B) elementAtIndex._asBuilder();
    }
  }

  /**
   * A holder for a list that can be mutable or immutable. This class allows the {@link
   * ModelListBuilder#models()} to be used to create a view of the current models in the models
   * list.
   *
   * @param <T>
   */
  static class ListHolder<T> implements List<T> {
    @Getter @Setter private List<T> delegate;

    private ListHolder(List<T> delegate) {
      this.delegate = delegate;
    }

    private void ensureMutable() {
      List<T> delegate = delegate();
      if (delegate instanceof ImmutableList<?>) {
        delegate(new ArrayList<>(delegate));
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
    public boolean contains(Object o) {
      return delegate.contains(o);
    }

    @Override
    public @NonNull Iterator<T> iterator() {
      return delegate.iterator();
    }

    @Override
    public @NonNull Object[] toArray() {
      return delegate.toArray();
    }

    @Override
    public @NonNull <T1> T1[] toArray(@NonNull T1[] a) {
      return delegate.toArray(a);
    }

    @Override
    public boolean add(T t) {
      return delegate.add(t);
    }

    @Override
    public boolean remove(Object o) {
      return delegate.remove(o);
    }

    @SuppressWarnings("SlowListContainsAll")
    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
      return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends T> c) {
      return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends T> c) {
      return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
      return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
      return delegate.retainAll(c);
    }

    @Override
    public void clear() {
      delegate.clear();
    }

    @Override
    public T get(int index) {
      return delegate.get(index);
    }

    @Override
    public T set(int index, T element) {
      return delegate.set(index, element);
    }

    @Override
    public void add(int index, T element) {
      delegate.add(index, element);
    }

    @Override
    public T remove(int index) {
      return delegate.remove(index);
    }

    @Override
    public int indexOf(Object o) {
      return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
      return delegate.lastIndexOf(o);
    }

    @Override
    public @NonNull ListIterator<T> listIterator() {
      return delegate.listIterator();
    }

    @Override
    public @NonNull ListIterator<T> listIterator(int index) {
      return delegate.listIterator(index);
    }

    @Override
    public @NonNull List<T> subList(int fromIndex, int toIndex) {
      return delegate.subList(fromIndex, toIndex);
    }
  }
}
