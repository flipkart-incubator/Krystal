package com.flipkart.krystal.vajram.protobuf3;

import com.flipkart.krystal.model.ImmutableModel;
import com.flipkart.krystal.model.Model;
import com.flipkart.krystal.model.ModelListBuilder;
import com.flipkart.krystal.model.UnmodifiableModelList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ProtoListBuilder<
        M extends Model, I extends ImmutableModel, B extends ImmutableModel.Builder>
    implements ModelListBuilder<M, I, B> {

  private final List<M> modelsDelegate;
  private final List<B> buildersDelegate;
  private final Consumer<M> addModel;
  private final Consumer<B> addBuilder;
  private final BiConsumer<Integer, M> addModelAtIndex;
  private final BiConsumer<Integer, B> addBuilderAtIndex;
  private final Predicate<Iterable<? extends M>> addAllModels;
  private final Runnable clear;
  private final BiConsumer<Integer, M> setModel;
  private final BiConsumer<Integer, B> setBuilder;
  private final Function<Integer, Model> remove;
  private final boolean builderExtendsModelRoot;
  private final ProtoListView<M, I> modelsView;

  public ProtoListBuilder(
      Class<M> modelType,
      Class<I> immutModelType,
      Class<B> builderType,
      List<M> modelsDelegate,
      List<B> buildersDelegate,
      Consumer<M> addModel,
      Consumer<B> addBuilder,
      BiConsumer<Integer, M> addModelAtIndex,
      BiConsumer<Integer, B> addBuilderAtIndex,
      Predicate<Iterable<? extends M>> addAllModels,
      Runnable clear,
      BiConsumer<Integer, M> setModel,
      BiConsumer<Integer, B> setBuilder,
      Function<Integer, Model> remove) {
    if (!modelType.isAssignableFrom(immutModelType)) {
      throw new IllegalArgumentException(
          "The Immutable Model type %s must be a subtype of the modelType %s"
              .formatted(immutModelType, modelType));
    }
    this.modelsDelegate = modelsDelegate;
    this.buildersDelegate = buildersDelegate;
    this.addModel = addModel;
    this.addBuilder = addBuilder;
    this.addModelAtIndex = addModelAtIndex;
    this.addBuilderAtIndex = addBuilderAtIndex;
    this.addAllModels = addAllModels;
    this.clear = clear;
    this.setModel = setModel;
    this.setBuilder = setBuilder;
    this.remove = remove;
    this.builderExtendsModelRoot = modelType.isAssignableFrom(builderType);
    this.modelsView = new ProtoListView<>(this, modelsDelegate);
  }

  @Override
  public ProtoListView<M, I> immutModelsView() {
    return modelsView;
  }

  public UnmodifiableModelList<M, I> unmodifiableModelsView() {
    return new UnmodifiableModelList<>(immutModelsView());
  }

  @Override
  public int size() {
    return modelsDelegate.size();
  }

  @Override
  public boolean isEmpty() {
    return modelsDelegate.isEmpty();
  }

  @Override
  public boolean addModel(M model) {
    addModel.accept(model);
    return true;
  }

  @Override
  public void addModel(int index, M element) {
    addModelAtIndex.accept(index, element);
  }

  @Override
  public boolean addBuilder(B b) {
    addBuilder.accept(b);
    return true;
  }

  @Override
  public void addBuilder(int index, B element) {
    addBuilderAtIndex.accept(index, element);
  }

  @Override
  public boolean addAllModels(Iterable<? extends M> i) {
    return addAllModels.test(i);
  }

  @Override
  public boolean addAllModels(int index, Iterable<? extends M> c) {
    boolean result = false;
    for (M m : c) {
      addModel(m);
      result = true;
    }
    return result;
  }

  @Override
  public boolean addAllBuilders(Iterable<? extends B> c) {
    boolean result = false;
    for (B b : c) {
      addBuilder(b);
      result = true;
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean addAllBuilders(int index, Iterable<? extends B> c) {
    if (builderExtendsModelRoot) {
      return addAllModels(index, (Iterable<? extends M>) c);
    }
    boolean result = false;
    int i = index;
    for (B b : c) {
      addBuilder(i++, b);
      result = true;
    }
    return result;
  }

  @Override
  public void clear() {
    clear.run();
  }

  @Override
  public M getModel(int index) {
    return modelsDelegate.get(index);
  }

  @Override
  public B getBuilder(int index) {
    return buildersDelegate.get(index);
  }

  @Override
  public void setModel(int index, M element) {
    setModel.accept(index, element);
  }

  @Override
  public void setBuilder(int index, B element) {
    setBuilder.accept(index, element);
  }

  @Override
  public Model remove(int index) {
    return remove.apply(index);
  }

  @Override
  public Iterator<M> modelsIterator() {
    return modelsDelegate.iterator();
  }

  @Override
  public Iterator<B> buildersIterator() {
    return buildersDelegate.iterator();
  }
}
