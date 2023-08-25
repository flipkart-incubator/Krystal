package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Inputs;
import com.google.common.collect.ImmutableList;
import java.util.List;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<Inputs> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency multiExecuteWith(List<Inputs> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(List<Inputs> inputs) implements ResolverCommand {
    public ImmutableList<Inputs> getInputs() {
      return ImmutableList.copyOf(inputs);
    }
  }
}
