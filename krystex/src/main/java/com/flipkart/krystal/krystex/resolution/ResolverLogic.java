package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.FacetValues;
import com.flipkart.krystal.data.ImmutableRequest;
import com.flipkart.krystal.facets.resolution.ResolverCommand;
import com.flipkart.krystal.krystex.Logic;
import java.util.List;

public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(
      List<? extends ImmutableRequest.Builder<?>> depRequests, FacetValues facetValues);
}
