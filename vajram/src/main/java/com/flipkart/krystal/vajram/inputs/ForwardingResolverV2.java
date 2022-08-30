package com.flipkart.krystal.vajram.inputs;

import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForwardingResolverV2<S, T> implements InputResolver {
  private final String targetName;
  private final InputId<T> target;
  private final @Nullable InputId<S> source;
  private final Function<?, Collection<T>> usingValues;

  public static <T> ForwardingResolverV2<T, T> forwardResolve(
      String inputName, InputId<T> target, InputId<T> source) {
    return new ForwardingResolverV2<>(inputName, target, source, identity());
  }

  public static <S, T> ForwardingResolverV2<S, T> forwardResolveValue(
      String inputName, InputId<T> target, InputId<S> source, Function<S, T> using) {
    return new ForwardingResolverV2<>(
        inputName, target, source, using.andThen(Collections::singleton));
  }

  public static <T> ForwardingResolverV2<Void, T> resolveValue(
      String inputName, InputId<T> target, Supplier<T> using) {
    return new ForwardingResolverV2<>(
        inputName,
        target,
        null,
        ((Function<Void, T>) t -> using.get()).andThen(Collections::singleton));
  }

  public static <S, T> ForwardingResolverV2<S, T> forwardResolveValues(
      String inputName, InputId<T> target, InputId<S> source, Function<S, Collection<T>> using) {
    return new ForwardingResolverV2<>(inputName, target, source, using);
  }

  @Override
  public QualifiedInputId resolutionTarget() {
    return new QualifiedInputId(targetName, target.vajramId(), target.inputName());
  }
}
