package com.flipkart.krystal.vajram.facets.resolution;

import com.flipkart.krystal.data.FacetValue;
import com.flipkart.krystal.data.FacetValue.SingleFacetValue;
import com.flipkart.krystal.data.FanoutDepResponses;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.FanoutTransformer;
import com.flipkart.krystal.vajram.facets.resolution.Transformer.One2OneTransformer;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

sealed interface Transformer<T> extends Function<FacetValue<?>, T>
    permits One2OneTransformer, FanoutTransformer {

  abstract sealed class One2OneTransformer implements Transformer<@Nullable Object> {}

  abstract sealed class FanoutTransformer implements Transformer<@Nullable Collection<?>> {}

  final class None2One extends One2OneTransformer {

    private final Supplier<?> logic;

    public None2One(Supplier<?> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.get();
    }
  }

  final class None2Many extends FanoutTransformer {

    private final Supplier<Collection<?>> logic;

    public None2Many(Supplier<Collection<?>> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Collection<?> apply(FacetValue<?> facetValue) {
      return logic.get();
    }
  }

  final class One2One extends One2OneTransformer {

    private final Function<SingleFacetValue<?>, ?> logic;

    public One2One(Function<SingleFacetValue<?>, ?> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((SingleFacetValue<?>) facetValue);
    }
  }

  final class One2Many extends FanoutTransformer {

    private final Function<SingleFacetValue<?>, ? extends Collection<?>> logic;

    public One2Many(Function<SingleFacetValue<?>, ? extends Collection<?>> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Collection<?> apply(FacetValue<?> facetValue) {
      return logic.apply((SingleFacetValue<?>) facetValue);
    }
  }

  final class Many2One extends One2OneTransformer {

    private final Function<FanoutDepResponses<?, ?>, ?> logic;

    public Many2One(Function<FanoutDepResponses<?, ?>, ?> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Object apply(FacetValue<?> facetValue) {
      return logic.apply((FanoutDepResponses<?, ?>) facetValue);
    }
  }

  final class Many2Many extends FanoutTransformer {

    private final Function<FanoutDepResponses<?, ?>, ? extends Collection<?>> logic;

    public Many2Many(Function<FanoutDepResponses<?, ?>, ? extends Collection<?>> logic) {
      this.logic = logic;
    }

    @Override
    public @Nullable Collection<?> apply(FacetValue<?> facetValue) {
      return logic.apply((FanoutDepResponses<?, ?>) facetValue);
    }
  }
}
