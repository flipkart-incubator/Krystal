package com.flipkart.krystal.model;

public interface ModelListBuilder<
    M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {
  static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelListBuilder<M, I, B> empty() {
    return BasicModelListBuilder.empty();
  }

  ImmutableModelList<M, I> immutModelsView();

  UnmodifiableModelList<M, I> unmodifiableModelsView();

  int size();

  boolean isEmpty();

  boolean addModel(M model);

  void addModel(int index, M element);

  boolean addBuilder(B b);

  void addBuilder(int index, B element);

  boolean addAllModels(Iterable<? extends M> c);

  boolean addAllModels(int index, Iterable<? extends M> c);

  boolean addAllBuilders(Iterable<? extends B> c);

  boolean addAllBuilders(int index, Iterable<? extends B> c);

  void clear();

  M getModel(int index);

  B getBuilder(int index);

  void setModel(int index, M element);

  void setBuilder(int index, B element);

  Model remove(int index);
}
