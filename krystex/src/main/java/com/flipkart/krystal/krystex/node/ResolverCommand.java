package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;

public interface ResolverCommand {

  ImmutableList<Inputs> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency executeWith(Inputs input) {
    return new ExecuteDependency(input);
  }

  static MultiExecuteDependency multiExecuteWith(ImmutableList<Inputs> inputs) {
    return new MultiExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(Inputs input) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return ImmutableList.of(input);
    }
  }

  record MultiExecuteDependency(ImmutableList<Inputs> inputs) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return inputs;
    }
  }
}
