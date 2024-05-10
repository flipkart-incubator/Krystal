package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Facets;
import com.google.common.collect.ImmutableList;

@SuppressWarnings("ClassReferencesSubclass")
@FunctionalInterface
public interface ResolverCommand {

  ImmutableList<Facets> getInputs();

  static SkipDependency skip(String reason) {
    return new SkipDependency(reason);
  }

  static ExecuteDependency multiExecuteWith(ImmutableList<Facets> inputs) {
    return new ExecuteDependency(inputs);
  }

  record SkipDependency(String reason) implements ResolverCommand {
    public ImmutableList<Facets> getInputs() {
      return ImmutableList.of();
    }
  }

  record ExecuteDependency(ImmutableList<Facets> inputs) implements ResolverCommand {
    public ImmutableList<Facets> getInputs() {
      return inputs;
    }
  }
}
