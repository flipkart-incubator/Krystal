package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.data.ImmutableRequest.Builder;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.Logic;
import com.google.common.collect.ImmutableList;
import java.util.List;

public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(
      List<? extends Builder<?>> depRequests, FacetValues facetValues);
}
