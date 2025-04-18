package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.FanoutDepResponses;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

sealed interface Transformer extends Function<FacetValue<?>, @Nullable Object> {
  boolean canFanout();

  record None2One(Supplier<?> logic) implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.get();
    }

    @Override
    public boolean canFanout() {
      return false;
    }
  }

  record None2Many(Supplier<Collection<?>> logic) implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.get();
    }

    @Override
    public boolean canFanout() {
      return true;
    }
  }

  record One2One(Function<SingleFacetValue<?>, ?> logic) implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((SingleFacetValue<?>) facetValue);
    }

    @Override
    public boolean canFanout() {
      return false;
    }
  }

  record One2Many(Function<SingleFacetValue<?>, ? extends Collection<?>> logic)
      implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((SingleFacetValue<?>) facetValue);
    }

    @Override
    public boolean canFanout() {
      return true;
    }
  }

  record Many2One(Function<FanoutDepResponses<?, ?>, ?> logic) implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((FanoutDepResponses<?, ?>) facetValue);
    }

    @Override
    public boolean canFanout() {
      return false;
    }
  }

  record Many2Many(Function<FanoutDepResponses<?, ?>, ? extends Collection<?>> logic)
      implements Transformer {
    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((FanoutDepResponses<?, ?>) facetValue);
    }

    @Override
    public boolean canFanout() {
      return true;
    }
  }
}
