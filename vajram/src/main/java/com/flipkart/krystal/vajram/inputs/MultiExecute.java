package com.flipkart.krystal.vajram.inputs;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public record MultiExecute<T>(Collection<T> multiInputs, boolean shouldSkip, String doc)
    implements DependencyCommand<T> {

  @Override
  public ImmutableCollection<Optional<T>> inputs() {
    return multiInputs.stream().map(Optional::ofNullable).collect(toImmutableList());
  }

  public static <T> MultiExecute<T> executeFanoutWith(Collection<T> inputs) {
    if (inputs.isEmpty()) {
      throw new IllegalArgumentException("Fanout cannot be executed with zero values");
    }
    return new MultiExecute<>(ImmutableList.copyOf(inputs), false, EMPTY_STRING);
  }

  public static <T> MultiExecute<T> skipFanout(String reason) {
    return new com.flipkart.krystal.vajram.inputs.MultiExecute<>(
        Collections.emptyList(), true, reason);
  }
}
