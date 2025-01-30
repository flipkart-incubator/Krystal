package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.Logic;
import com.google.common.collect.ImmutableList;

public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(ImmutableList<? extends Builder> depRequests, Facets facets);
}
