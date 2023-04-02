package com.flipkart.krystal.caramel.model;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @param <ROOT> Workflow Root Context Type
 */
public interface WorkflowBuildStage<INPUT, ROOT extends WorkflowPayload> extends Consumer<ROOT> {

  WorkflowBuildStage<INPUT, ROOT> peek(Runnable runnable);

  <T> WorkflowBuildStage<INPUT, ROOT> peek(
      Field<T, ? super ROOT> sourceField, Consumer<T> consumer);

  <T1, T2> WorkflowBuildStage<INPUT, ROOT> peek(
      Field<T1, ? super ROOT> sourceField1,
      Field<T2, ? super ROOT> sourceField2,
      BiConsumer<T1, T2> consumer);

  WorkflowBuildStage<INPUT, ROOT> peek(
      List<Field<?, ? super ROOT>> sourceFields, Consumer<ROOT> consumer);

  ForkStage<INPUT, ROOT> fork();

  <U> WorkflowBuildStage<INPUT, ROOT> compute(Field<U, ROOT> targetField, Supplier<U> supplier);

  <U, S> WorkflowBuildStage<INPUT, ROOT> compute(
      Field<U, ROOT> targetField, Function<S, U> computer, Field<S, ? super ROOT> sourceField);

  <U> WorkflowBuildStage<INPUT, ROOT> compute(
      Field<U, ROOT> targetField,
      Function<ROOT, U> computer,
      List<Field<?, ? super ROOT>> sourceFields);

  <S, ITEM, OUTPUT> SplitExtractStage<INPUT, ROOT, OUTPUT> iterate(
      Field<S, ROOT> sourceField,
      Function<S, ? extends Stream<? super ITEM>> splitter,
      Function<ITEM, OUTPUT> processor);

  <S, ITEM, OUTPUT> SplitExtractStage<INPUT, ROOT, OUTPUT> iterate(
      List<Field<?, ? super ROOT>> sourceFields,
      Function<ROOT, ? extends Stream<? super ITEM>> splitter,
      Function<ITEM, OUTPUT> processor);

  default ThenStage<INPUT, ROOT> ifTrue(
      Field<Boolean, ROOT> condition,
      Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> ifTrue) {
    return ifTrue(condition, aBoolean -> aBoolean, ifTrue);
  }

  <T> ThenStage<INPUT, ROOT> ifTrue(
      Field<T, ROOT> sourceField,
      Predicate<T> predicate,
      Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> ifTrue);

  default WorkflowBuildStage<INPUT, ROOT> conditional(
      Field<Boolean, ROOT> source,
      Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> ifTrue,
      @Nullable Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>>
          ifFalse) {
    return conditional(source, aBoolean -> aBoolean, ifTrue, ifFalse);
  }

  <T> WorkflowBuildStage<INPUT, ROOT> conditional(
      Field<T, ROOT> source,
      Predicate<T> condition,
      Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> ifTrue,
      @Nullable Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> ifFalse);

  <O> TerminatedWorkflow<INPUT, ROOT, O> terminateWithOutput(Field<O, ROOT> outputField);

  WorkflowBuildStage<INPUT, ROOT> checkpoint(String name);

  interface ThenStage<INPUT, ROOT extends WorkflowPayload> {
    <T> ThenStage<INPUT, ROOT> elseIfTrue(
        Field<T, ROOT> sourceField,
        Predicate<T> condition,
        Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> elseIfTrue);

    WorkflowBuildStage<INPUT, ROOT> orElse(
        Function<WorkflowBuildStage<INPUT, ROOT>, WorkflowBuildStage<INPUT, ROOT>> orElse);

    WorkflowBuildStage<INPUT, ROOT> endIf();
  }
}
