package com.flipkart.krystal.model.list;

import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.ImmutableModel.Builder;
import com.flipkart.krystal.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import lombok.Getter;
import lombok.Setter;

final class BasicModelsListBuilder<M extends Model, I extends ImmutableModel, B extends Builder>
    implements ModelsListBuilder<M, I, B> {

  public static <M extends Model, I extends ImmutableModel, B extends Builder>
      BasicModelsListBuilder<M, I, B> empty() {
    return new BasicModelsListBuilder<>(ImmutableList.of());
  }

  /**
   * A list which can contain either an immutable model or it's builder. Immutable models are
   * replaced with Builders on-demand when the value needs to be mutated. This strategy allows us to
   * prevent unnecessary object creation unless actually needed.
   */
  private final ListHolder<Model> models;

  private final ModelsListView<M, I> immutModelsView;
  private final UnmodifiableModelsList<M, I> unmodifiableModelsView;

  @SuppressWarnings("unchecked")
  private BasicModelsListBuilder(List<M> models) {
    ListHolder<M> listHolder;
    if (models instanceof ImmutableList) {
      listHolder = new ListHolder<>(models);
    } else {
      listHolder = new ListHolder<>(new ArrayList<>(models));
    }
    this.models = (ListHolder<Model>) listHolder;
    this.immutModelsView = new ModelsListView<>(this, unmodifiableList(listHolder));
    this.unmodifiableModelsView = new UnmodifiableModelsList<>(immutModelsView);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ModelsListView<M, I> immutModelsView() {
    return immutModelsView;
  }

  @SuppressWarnings("unchecked")
  @Override
  public UnmodifiableModelsList<M, I> unmodifiableModelsView() {
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
    return models.add(model);
  }

  @Override
  public void addModel(int index, M element) {
    models.add(index, element);
  }

  @Override
  public boolean addBuilder(B b) {
    return models.add(b._build());
  }

  @Override
  public void addBuilder(int index, B element) {
    models.add(index, element);
  }

  @Override
  public boolean addAllModels(Iterable<? extends M> iterable) {
    if (iterable instanceof Collection<? extends M> c) {
      return models.addAll(c);
    }
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    for (M m : iterable) {
      models.add(m);
    }
    return true;
  }

  @Override
  public boolean addAllModels(int index, Iterable<? extends M> iterable) {
    if (iterable instanceof Collection<? extends M> c) {
      return models.addAll(index, c);
    }
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    int i = index;
    for (M m : iterable) {
      models.add(i++, m);
    }
    return true;
  }

  @Override
  public boolean addAllBuilders(Iterable<? extends B> iterable) {
    if (iterable instanceof Collection<? extends B> c) {
      return models.addAll(c);
    }
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    for (B b : iterable) {
      models.add(b);
    }
    return true;
  }

  @Override
  public boolean addAllBuilders(int index, Iterable<? extends B> iterable) {
    if (iterable instanceof Collection<? extends B> c) {
      return models.addAll(index, c);
    }
    if (!iterable.iterator().hasNext()) {
      return false;
    }
    int i = index;
    for (B b : iterable) {
      models.add(i++, b);
    }
    return true;
  }

  @Override
  public void clear() {
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
    models.set(index, element);
  }

  @Override
  public void setBuilder(int index, B element) {
    models.set(index, element);
  }

  @Override
  public Model remove(int index) {
    return models.remove(index);
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
    public Iterator<T> iterator() {
      if (!delegate.isEmpty()) {
        ensureMutable();
      }
      return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
      return delegate.toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
      return delegate.toArray(a);
    }

    @Override
    public boolean add(T t) {
      ensureMutable();
      return delegate.add(t);
    }

    @Override
    public boolean remove(Object o) {
      ensureMutable();
      return delegate.remove(o);
    }

    @SuppressWarnings("SlowListContainsAll")
    @Override
    public boolean containsAll(Collection<?> c) {
      return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
      ensureMutable();
      return delegate.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
      if (c.isEmpty()) {
        return false;
      }
      ensureMutable();
      return delegate.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
      if (c.isEmpty()) {
        return false;
      }
      ensureMutable();
      return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
      ensureMutable();
      return delegate.retainAll(c);
    }

    @Override
    public void clear() {
      if (!delegate.isEmpty()) {
        ensureMutable();
        delegate.clear();
      }
    }

    @Override
    public T get(int index) {
      return delegate.get(index);
    }

    @Override
    public T set(int index, T element) {
      ensureMutable();
      return delegate.set(index, element);
    }

    @Override
    public void add(int index, T element) {
      ensureMutable();
      delegate.add(index, element);
    }

    @Override
    public T remove(int index) {
      ensureMutable();
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
    public ListIterator<T> listIterator() {
      ensureMutable();
      return delegate.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
      ensureMutable();
      return delegate.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
      return delegate.subList(fromIndex, toIndex);
    }
  }
}
