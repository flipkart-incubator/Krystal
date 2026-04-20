package com.flipkart.krystal.model.list;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;

public interface ModelsListBuilder<
    M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder> {
  static <M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
      ModelsListBuilder<M, I, B> empty() {
    return BasicModelsListBuilder.empty();
  }

  ImmutModelsListView<M, I> immutModelsView();

  UnmodifiableModelsList<M, I> unmodifiableModelsView();

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
