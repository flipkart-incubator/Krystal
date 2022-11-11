package com.flipkart.krystal.vajram.inputs;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ForwardingResolverV2<S, T> implements InputResolver {
  private final String dependencyName;
  private final InputId<T> target;
  private final @Nullable InputId<S> source;
  private final Function<?, Collection<T>> usingValues;

  public static <T> ForwardingResolverV2<T, T> forwardResolve(
      String dependencyName, InputId<T> target, InputId<T> source) {
    return new ForwardingResolverV2<>(dependencyName, target, source, (T o) -> Set.of(o));
  }

  public static <S, T> ForwardingResolverV2<S, T> forwardResolveValue(
      String dependencyName, InputId<T> target, InputId<S> source, Function<S, T> using) {
    return new ForwardingResolverV2<>(
        dependencyName, target, source, using.andThen(Collections::singleton));
  }

  public static <T> ForwardingResolverV2<Void, T> resolveValue(
      String dependencyName, InputId<T> target, Supplier<T> using) {
    return new ForwardingResolverV2<>(
        dependencyName,
        target,
        null,
        ((Function<Void, T>) t -> using.get()).andThen(Collections::singleton));
  }

  public static <S, T> ForwardingResolverV2<S, T> forwardResolveValues(
      String dependencyName,
      InputId<T> target,
      InputId<S> source,
      Function<S, Collection<T>> using) {
    return new ForwardingResolverV2<>(dependencyName, target, source, using);
  }

  @Override
  public ImmutableSet<String> sources() {
    if (source == null) {
      return ImmutableSet.of();
    }
    return ImmutableSet.of(source.inputName());
  }

  @SuppressWarnings("unchecked")
  @Override
  public Function<?, Collection<T>> transformationLogic() {
    if (this.usingValues == null) {
      return o -> Collections.emptyList();
    }
    return this.usingValues;
  }

  @Override
  public QualifiedInputs resolutionTarget() {
    return new QualifiedInputs(dependencyName, target.accessSpec(), target.inputName());
  }
}
