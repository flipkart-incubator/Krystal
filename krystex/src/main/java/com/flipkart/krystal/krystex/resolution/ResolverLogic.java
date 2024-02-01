package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Facets;
import com.flipkart.krystal.krystex.Logic;

@FunctionalInterface
public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(Facets facets);
}
