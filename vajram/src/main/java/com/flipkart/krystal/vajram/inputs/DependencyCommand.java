package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyCommand<T> {

  ImmutableCollection<Optional<T>> inputs();

  static <T> Skip<T> skip(String reason) {
    return new Skip<>(reason);
  }

  static <T> Execute<T> executeWith(@Nullable T value) {
    return new Execute<>(value);
  }

  static <T> MultiExecute<T> multiExecuteWith(Collection<T> inputs) {
    return new MultiExecute<>(ImmutableList.copyOf(inputs));
  }

  record Skip<T>(String reason) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<Optional<T>> inputs() {
      return ImmutableList.of();
    }

    @SuppressWarnings("unchecked")
    public <U> Skip<U> cast() {
      return (Skip<U>) this;
    }
  }

  record Execute<T>(T input) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<Optional<T>> inputs() {
      return ImmutableList.of(Optional.ofNullable(input));
    }
  }

  record MultiExecute<T>(Collection<T> multiInputs) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<Optional<T>> inputs() {
      return multiInputs.stream().map(Optional::ofNullable).collect(toImmutableList());
    }
  }
}
