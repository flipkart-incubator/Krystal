package com.flipkart.krystal.model;

import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PACKAGE;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ModelListBuilder<
    M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {

  public static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelListBuilder<M, I, B> empty() {
    return ofModels(ImmutableList.of());
  }

  @SuppressWarnings("unchecked")
  public static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelListBuilder<M, I, B> ofModels(@Nullable List<M> models) {
    if (models == null) {
      return new ModelListBuilder<>(ImmutableList.of());
    }
    if (models instanceof SimpleModelList uList) {
      return (ModelListBuilder<M, I, B>) uList.source();
    }
    return new ModelListBuilder<>(models);
  }

  @Getter(PACKAGE)
  private List<Model> models;

  @SuppressWarnings("unchecked")
  private ModelListBuilder(List<M> models) {
    if (models instanceof ImmutableList) {
      this.models = (List<Model>) models;
    } else {
      this.models = new ArrayList<>(models);
    }
  }

  public int size() {
    return models.size();
  }

  public boolean isEmpty() {
    return models.isEmpty();
  }

  @SuppressWarnings("unchecked")
  public boolean addModel(M m) {
    ensureMutable();
    return models.add(m);
  }

  public void addModel(int index, M element) {
    ensureMutable();
    models.add(index, element);
  }

  @SuppressWarnings("unchecked")
  public boolean addBuilder(B b) {
    ensureMutable();
    return models.add(b);
  }

  public void addBuilder(int index, B element) {
    ensureMutable();
    models.add(index, element);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllModels(Collection<? extends M> c) {
    ensureMutable();
    return models.addAll(c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllModels(int index, Collection<? extends M> c) {
    ensureMutable();
    return models.addAll(index, c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllBuilders(Collection<? extends B> c) {
    ensureMutable();
    return models.addAll(c);
  }

  @SuppressWarnings("unchecked")
  public boolean addAllBuilders(int index, Collection<? extends B> c) {
    ensureMutable();
    return models.addAll(index, c);
  }

  public void clear() {
    ensureMutable();
    models.clear();
  }

  public B get(int index) {
    return ensureBuilderAtIndex(index);
  }

  @SuppressWarnings("unchecked")
  public void setModel(int index, M element) {
    ensureMutable();
    models.set(index, element);
  }

  @SuppressWarnings("unchecked")
  public void setBuilder(int index, B element) {
    ensureMutable();
    models.set(index, element);
  }

  public Model remove(int index) {
    ensureMutable();
    return models.remove(index);
  }

  @SuppressWarnings("unchecked")
  public Iterator<B> iterator() {
    for (int i = 0; i < models.size(); i++) {
      ensureBuilderAtIndex(i);
    }
    return Iterators.transform(models.iterator(), model -> (B) requireNonNull(model)._asBuilder());
  }

  public SimpleModelList<M, I> asImmutModelList() {
    return new SimpleModelList<>(this);
  }

  @SuppressWarnings("unchecked")
  private void ensureMutable() {
    if (models instanceof ImmutableList<?>) {
      models = new ArrayList(models);
    }
  }

  @SuppressWarnings("unchecked")
  private B ensureBuilderAtIndex(int i) {
    Object elementAtIndex = models.get(i);
    if (elementAtIndex instanceof ImmutableModel _immut) {
      ImmutableModel.Builder builder = _immut._asBuilder();
      models.set(i, builder);
      return (B) builder;
    } else if (elementAtIndex instanceof Model model) {
      return (B) model._asBuilder();
    }
    return null;
  }
}
