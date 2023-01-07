package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableList;

public interface ResolverCommand {

  ImmutableList<NodeInputs> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency executeWith(NodeInputs input) {
    return new ExecuteDependency(input);
  }

  static MultiExecuteDependency multiExecuteWith(ImmutableList<NodeInputs> inputs) {
    return new MultiExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<NodeInputs> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(NodeInputs input) implements ResolverCommand {
    public ImmutableList<NodeInputs> getInputs() {
      return ImmutableList.of(input);
    }
  }

  record MultiExecuteDependency(ImmutableList<NodeInputs> inputs) implements ResolverCommand {
    public ImmutableList<NodeInputs> getInputs() {
      return inputs;
    }
  }
}
