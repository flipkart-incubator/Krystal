package com.flipkart.krystal.krystex;

import com.flipkart.krystal.data.Inputs;

public non-sealed interface ResolverLogic extends Logic {
  ResolverCommand resolve(Inputs inputs);
}
