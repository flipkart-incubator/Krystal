package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;

@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<Inputs> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency multiExecuteWith(ImmutableList<Inputs> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(ImmutableList<Inputs> inputs) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return inputs;
    }
  }
}
