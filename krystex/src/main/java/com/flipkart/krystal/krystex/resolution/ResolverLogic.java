package com.flipkart.krystal.krystex.resolution;

import com.flipkart.krystal.data.Inputs;
import com.flipkart.krystal.krystex.Logic;

@FunctionalInterface
public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(Inputs inputs);
}
