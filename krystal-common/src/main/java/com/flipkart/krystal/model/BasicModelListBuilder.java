package com.flipkart.krystal.model;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

import com.flipkart.krystal.model.ImmutableModel.Builder;
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

final class BasicModelListBuilder<M extends Model, I extends ImmutableModel, B extends Builder>
    implements ModelListBuilder<M, I, B> {

  public static <M extends Model, I extends ImmutableModel, B extends Builder>
      BasicModelListBuilder<M, I, B> empty() {
    return new BasicModelListBuilder<>(ImmutableList.of());
  }

  /**
   * A list which can contain either an immutable model or it's builder. Immutable models are
   * replaced with Builders on-demand when the value needs to be mutated. This strategy allows us to
   * prevent unnecessary object creation unless actually needed.
   */
  private final ListHolder<Model> models;

  private final ModelsListView<M, I> immutModelsView;
  private final UnmodifiableModelList<M, I> unmodifiableModelsView;

  @SuppressWarnings("unchecked")
  private BasicModelListBuilder(List<M> models) {
    ListHolder<M> listHolder;
    if (models instanceof ImmutableList) {
      listHolder = new ListHolder<>(models);
    } else {
      listHolder = new ListHolder<>(new ArrayList<>(models));
    }
    this.models = (ListHolder<Model>) listHolder;
    this.immutModelsView = new ModelsListView<>(this, unmodifiableList(listHolder));
    this.unmodifiableModelsView = new UnmodifiableModelList<>(immutModelsView);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ModelsListView<M, I> immutModelsView() {
    return immutModelsView;
  }

  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableModelList<M, I> unmodifiableModelsView() {
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
  public boolean addModel(M model) {
    ensureMutable();
    return models.add(model);
  }

  @Override
  public void addModel(int index, M element) {
    ensureMutable();
    models.add(index, element);
  }

  @Override
  public boolean addBuilder(B b) {
    ensureMutable();
    return models.add(b);
  }

  @Override
  public void addBuilder(int index, B element) {
    ensureMutable();
    models.add(index, element);
  }

  @Override
  public boolean addAllModels(Iterable<? extends M> iterable) {
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    ensureMutable();
    if (iterable instanceof Collection<? extends M> c) {
      return models.addAll(c);
    }
    for (M m : iterable) {
      models.add(m);
    }
    return true;
  }

  @Override
  public boolean addAllModels(int index, Iterable<? extends M> iterable) {
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    ensureMutable();
    if (iterable instanceof Collection<? extends M> c) {
      return models.addAll(index, c);
    }
    int i = index;
    for (M m : iterable) {
      models.add(i++, m);
    }
    return true;
  }

  @Override
  public boolean addAllBuilders(Iterable<? extends B> iterable) {
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    ensureMutable();
    if (iterable instanceof Collection<? extends B> c) {
      return models.addAll(c);
    }
    for (B b : iterable) {
      models.add(b);
    }
    return true;
  }

  @Override
  public boolean addAllBuilders(int index, Iterable<? extends B> iterable) {
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    ensureMutable();
    if (iterable instanceof Collection<? extends B> c) {
      return models.addAll(index, c);
    }
    int i = index;
    for (B b : iterable) {
      models.add(i++, b);
    }
    return true;
  }

  @Override
  public void clear() {
    ensureMutable();
    models.clear();
  }

  @SuppressWarnings("unchecked")
  @Override
  public M getModel(int index) {
    Model model = models.get(index);
    if (model instanceof ImmutableModel) {
      return (M) model;
    } else {
      return (M) model._build();
    }
  }

  @Override
  public B getBuilder(int index) {
    return ensureBuilderAtIndex(index);
  }

  @Override
  public void setModel(int index, M element) {
    ensureMutable();
    models.set(index, element);
  }

  @Override
  public void setBuilder(int index, B element) {
    ensureMutable();
    models.set(index, element);
  }

  @Override
  public Model remove(int index) {
    ensureMutable();
    return models.remove(index);
  }

  @Override
  public Iterator<M> modelsIterator() {
    return null;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<B> buildersIterator() {
    for (int i = 0; i < models.size(); i++) {
      ensureBuilderAtIndex(i);
    }
    return Iterators.transform(models.iterator(), model -> (B) requireNonNull(model)._asBuilder());
  }

  private void ensureMutable() {
    models.ensureMutable();
  }

  @SuppressWarnings("unchecked")
  private B ensureBuilderAtIndex(int i) {
    Model elementAtIndex = models.get(i);
    if (elementAtIndex instanceof ImmutableModel _immut) {
      ensureMutable();
      Builder builder = _immut._asBuilder();
      models.set(i, builder);
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
  private static class ListHolder<T> implements List<T> {
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
