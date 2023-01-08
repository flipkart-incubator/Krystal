package com.flipkart.krystal.krystex.node;

import com.flipkart.krystal.data.Inputs;

public interface ResolverNodeLogic {
  ResolverCommand resolve(Inputs nodeInputs);
}
