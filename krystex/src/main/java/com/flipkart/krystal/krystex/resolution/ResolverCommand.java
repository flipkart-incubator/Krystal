package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Request;
import com.google.common.collect.ImmutableList;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<? extends Request<Object>> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency multiExecuteWith(ImmutableList<? extends Request<Object>> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Request<Object>> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(ImmutableList<? extends Request<Object>> inputs)
      implements ResolverCommand {
    public ImmutableList<? extends Request<Object>> getInputs() {
      return inputs;
    }
  }
}
