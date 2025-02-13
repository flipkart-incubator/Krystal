package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.Errable;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

sealed interface Transformer extends Function<List<Errable<?>>, @Nullable Object> {
  record One2One(Function<List<Errable<?>>, ?> logic) implements Transformer {

    @Override
    public @Nullable Object apply(List<Errable<?>> errables) {
      return logic.apply(errables);
    }
  }

  record Fanout(Function<List<Errable<?>>, ? extends Collection<?>> logic) implements Transformer {

    @Override
    public @Nullable Object apply(List<Errable<?>> errables) {
      return logic.apply(errables);
    }
  }
}
