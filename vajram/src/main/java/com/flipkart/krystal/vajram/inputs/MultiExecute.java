package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record MultiExecute<T>(Collection<T> multiInputs, boolean shouldSkip, String doc)
    implements DependencyCommand<T> {

  @Override
  public ImmutableCollection<Optional<T>> inputs() {
    List<Optional<T>> collection = new ArrayList<>();
    multiInputs.forEach(
        input -> {
          collection.add(Optional.ofNullable(input));
        });
    return ImmutableList.copyOf(collection);
  }

  public static <T> MultiExecute<T> executeFanoutWith(Collection<T> inputs) {
    return new MultiExecute<>(ImmutableList.copyOf(inputs), false, EMPTY_STRING);
  }

  public static <T> MultiExecute<T> skipFanout(String reason) {
    return new com.flipkart.krystal.vajram.inputs.MultiExecute<>(
        Collections.emptyList(), true, reason);
  }
}
