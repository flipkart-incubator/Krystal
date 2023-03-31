package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DependencyCommand<T> {

  String EMPTY_STRING = "";

  ImmutableCollection<Optional<T>> inputs();

  boolean shouldSkip();

  String doc();

  static <T> SingleExecute<T> singleExecuteWith(@Nullable T value) {
    return new SingleExecute<>(value, false, EMPTY_STRING);
  }

  static <T> MultiExecute<T> multiExecuteWith(Collection<T> inputs) {
    return new MultiExecute<>(ImmutableList.copyOf(inputs), false, EMPTY_STRING);
  }

  record SingleExecute<T>(T input, boolean shouldSkip, String doc) implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<Optional<T>> inputs() {
      return ImmutableList.of(Optional.ofNullable(input));
    }

    public static <T> SingleExecute<T> skipSingleExecute(String reason) {
      return new SingleExecute<>(null, true, reason);
    }
  }

  record MultiExecute<T>(Collection<T> multiInputs, boolean shouldSkip, String doc)
      implements DependencyCommand<T> {

    @Override
    public ImmutableCollection<Optional<T>> inputs() {
      return multiInputs.stream().map(Optional::ofNullable).collect(toImmutableList());
    }

    public static <T> MultiExecute<T> skipMultiExecute(String reason) {
      return new MultiExecute<>(Collections.emptyList(), true, reason);
    }
  }
}
