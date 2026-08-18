package com.flipkart.krystal.facets.resolution;

import static java.util.Collections.unmodifiableList;

import com.flipkart.krystal.data.Errable;
import com.flipkart.krystal.data.ImmutableRequest;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassReferencesSubclass")
public sealed interface ResolverCommand {

  List<? extends ImmutableRequest.Builder<?>> getRequests();

  static SkipDependency skip(String reason) {
    return skip(reason, null);
  }

  static SkipDependency skip(String reason, @Nullable Throwable skipCause) {
    return new SkipDependency(reason, skipCause);
  }

  static ExecuteDependency executeWithRequests(List<? extends ImmutableRequest.Builder<?>> inputs) {
    return new ExecuteDependency(unmodifiableList(inputs));
  }

  static ExecuteDependency executeWithErrables(List<? extends Errable<? extends ImmutableRequest.Builder<?>>> inputs) {
    return new ExecuteDependency(unmodifiableList(inputs));
  }

  record SkipDependency(String reason, @Nullable Throwable cause) implements ResolverCommand {
    @Override
    public ImmutableList<? extends ImmutableRequest.Builder<?>> getRequests() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(List<? extends ImmutableRequest.Builder<?>> requests)
      implements ResolverCommand {
    @Override
    public List<? extends ImmutableRequest.Builder<?>> getRequests() {
      return requests;
    }
  }
}
