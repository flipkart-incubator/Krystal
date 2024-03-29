package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.Logic;
import com.google.common.collect.ImmutableMap;
import java.util.List;

@FunctionalInterface
public non-sealed interface MultiResolver extends Logic {
  ImmutableMap<String, ResolverCommand> resolve(
      List<DependencyResolutionRequest> resolverRequests, Facets facets);
}
