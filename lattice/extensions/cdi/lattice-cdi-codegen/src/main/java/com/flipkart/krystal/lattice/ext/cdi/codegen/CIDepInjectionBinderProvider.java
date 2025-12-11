package com.flipkart.krystal.lattice.ext.cdi.codegen;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsContainer;
import com.flipkart.krystal.lattice.codegen.spi.di.BindingsProvider;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;

@AutoService(BindingsProvider.class)
public final class CIDepInjectionBinderProvider implements BindingsProvider {

  @Override
  public ImmutableList<BindingsContainer> bindings(LatticeCodegenContext context) {
    return ImmutableList.of();
  }
}
