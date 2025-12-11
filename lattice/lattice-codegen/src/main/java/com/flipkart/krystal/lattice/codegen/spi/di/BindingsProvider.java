package com.flipkart.krystal.lattice.codegen.spi.di;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.common.collect.ImmutableList;

public interface BindingsProvider {
  ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context);
}
