package com.flipkart.krystal.resolution;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<? extends Request<Object>> getRequests();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency computedRequests(ImmutableList<? extends Request<Object>> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Request<Object>> getRequests() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(ImmutableList<? extends Request<Object>> requests)
      implements ResolverCommand {
    public ImmutableList<? extends Request<Object>> getRequests() {
      return requests;
    }
  }
}
