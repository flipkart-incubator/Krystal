package com.flipkart.krystal.vajram.inputs.resolution;

import com.flipkart.krystal.data.ValueOrError;
import com.flipkart.krystal.vajram.Vajram;
import com.flipkart.krystal.vajram.inputs.VajramInputTypeSpec;
import com.flipkart.krystal.vajram.inputs.resolution.ResolverStage.AsIsResolverStage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @param <S> Source Type: The DataType of the source input being used for resolution
 * @param <T> Target Type: The DataType of the dependency target input being resolved
 * @param <CV> CurrentVajram: The current vajram which is resolving the input
 * @param <DV> DependencyVajram: The vajram whose input is being resolved
 */
public sealed class ResolverStage<S, T, CV extends Vajram<?>, DV extends Vajram<?>>
    permits AsIsResolverStage {
  private final VajramInputTypeSpec<T, DV> targetInput;
  private final VajramInputTypeSpec<S, CV> sourceInput;
  private final List<SkipPredicate<S>> skipConditions = new ArrayList<>();

  ResolverStage(VajramInputTypeSpec<T, DV> targetInput, VajramInputTypeSpec<S, CV> sourceInput) {
    this.targetInput = targetInput;
    this.sourceInput = sourceInput;
  }

  /**
   * @param whenToSkip The condition when the dependency needs to be skipped
   * @param reason The reason for skipping the dependency
   */
  public ResolverStage<S, T, CV, DV> skipIf(Predicate<Optional<S>> whenToSkip, String reason) {
    this.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
    return this;
  }

  /**
   * @param transformer The logic to use to tranform the source data type {@code S} to a single
   *     value of target data type {@code T} (no fanout)
   * @return The resultant {@link SimpleInputResolverSpec}
   */
  public SimpleInputResolverSpec<S, T, CV, DV> with(
      Function<Optional<S>, @Nullable T> transformer) {
    return new SimpleInputResolverSpec<>(
        targetInput, sourceInput, skipConditions, transformer, null);
  }

  public static final class AsIsResolverStage<T, CV extends Vajram<?>, DV extends Vajram<?>>
      extends ResolverStage<T, T, CV, DV> {
    AsIsResolverStage(
        VajramInputTypeSpec<T, DV> targetInput, VajramInputTypeSpec<T, CV> sourceInput) {
      super(targetInput, sourceInput);
    }

    @Override
    public AsIsResolverStage<T, CV, DV> skipIf(Predicate<Optional<T>> whenToSkip, String reason) {
      super.skipConditions.add(new SkipPredicate<>(reason, whenToSkip));
      return this;
    }

    public SimpleInputResolverSpec<T, T, CV, DV> asResolver() {
      return with(t -> t.orElse(null));
    }
  }

  /**
   * The stage which can be used to further specify the non-fanout resolver of the given targetInput
   *
   * @param <T> The data type of the input being resolved.
   * @param <DV> The dependency whose input is being resolved.
   */
  public static final class ResolveStage<T, DV extends Vajram<?>> {
    private final VajramInputTypeSpec<T, DV> targetInput;

    ResolveStage(VajramInputTypeSpec<T, DV> targetInput) {
      this.targetInput = targetInput;
    }

    /**
     * Use the value of the source input as-is, without any transformation. If the source input is
     * {@link ValueOrError#empty()}, then the resolved value will also be {@link
     * ValueOrError#empty()}. This is possible only if the dataType of the sourceInput and the
     * target input are same.
     *
     * @see #using(VajramInputTypeSpec)
     * @param sourceInput the spec of the source input being used for resolution
     */
    public <CV extends Vajram<?>> AsIsResolverStage<T, CV, DV> usingAsIs(
        VajramInputTypeSpec<T, CV> sourceInput) {
      return new AsIsResolverStage<>(targetInput, sourceInput);
    }

    /**
     * Use the value of the source input and transform it to compute the resolved value.
     *
     * @param sourceInput the spec of the source input whose value is used to resolve the dependency
     *     input.
     */
    public <S, CV extends Vajram<?>> ResolverStage<S, T, CV, DV> using(
        VajramInputTypeSpec<S, CV> sourceInput) {
      return new ResolverStage<>(targetInput, sourceInput);
    }

    /**
     * Creates a resolver spec which does not have any source. This is useful when we want to
     * statically bind a value to a depdency input
     *
     * @param with a supplier which provides the value which is used to resolve the dependency input
     * @return The resultant {@link SimpleInputResolverSpec}
     * @param <CV> The current vajram which is doing the resolution
     */
    public <CV extends Vajram<?>> SimpleInputResolverSpec<Void, T, CV, DV> with(Supplier<T> with) {
      return new SimpleInputResolverSpec<>(targetInput, null, List.of(), o -> with.get(), null);
    }
  }
}
