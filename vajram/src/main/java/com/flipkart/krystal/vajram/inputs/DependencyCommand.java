package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;

public interface DependencyCommand<T> {

  ImmutableCollection<T> inputs();

  static <T> Skip<T> skip(String reason) {
    return new Skip<>(reason);
  }

  static <T> Execute<T> executeWith(T value) {
    return new Execute<>(value);
  }

  static <T> MultiExecute<T> multiExecuteWith(Collection<T> inputs) {
    return new MultiExecute<>(ImmutableList.copyOf(inputs));
  }

  record Skip<T>(String reason) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<T> inputs() {
      return ImmutableList.of();
    }
  }

  record Execute<T>(T input) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<T> inputs() {
      return ImmutableList.of(input);
    }
  }

  record MultiExecute<T>(ImmutableCollection<T> inputs) implements DependencyCommand<T> {}
}
