package com.flipkart.krystal.resolution;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<? extends Request<?>> getRequests();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency executeWithRequests(ImmutableList<? extends Request<?>> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<? extends Request<?>> getRequests() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(ImmutableList<? extends Request<?>> requests)
      implements ResolverCommand {
    public ImmutableList<? extends Request<?>> getRequests() {
      return requests;
    }
  }
}
