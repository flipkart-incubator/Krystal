package com.flipkart.krystal.facets.resolution;

import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  List<? extends ImmutableRequest.Builder<?>> getRequests();

  static SkipDependency skip(String reason) {
    return skip(reason, null);
  }

  static SkipDependency skip(String reason, @Nullable Throwable skipCause) {
    return new SkipDependency(reason, skipCause);
  }

  static ExecuteDependency executeWithRequests(List<? extends @NonNull Builder<?>> inputs) {
    return new ExecuteDependency(inputs);
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
