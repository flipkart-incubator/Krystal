package com.flipkart.krystal.krystex.node;

import com.google.common.collect.ImmutableList;
import java.util.Collection;

public interface ResolverCommand {

  ImmutableList<NodeInputs> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency executeWith(NodeInputs input) {
    return new ExecuteDependency(input);
  }

  static MultiExecuteDependency multiExecuteWith(Collection<NodeInputs> inputs) {
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

  record MultiExecuteDependency(Collection<NodeInputs> inputs) implements ResolverCommand {
    public ImmutableList<NodeInputs> getInputs() {
      return ImmutableList.copyOf(inputs);
    }
  }
}
