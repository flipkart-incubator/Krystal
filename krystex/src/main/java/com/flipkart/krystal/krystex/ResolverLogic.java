package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;

@FunctionalInterface
public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand execute(Inputs inputs);
}
