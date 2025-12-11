package com.flipkart.krystal.lattice.codegen.spi.di;

import static com.flipkart.krystal.lattice.codegen.DepInjectionFramework.NONE;

import com.flipkart.krystal.lattice.codegen.DepInjectionFramework;

public sealed interface Binding permits ImplTypeBinding, NullBinding, ProviderMethod {
  String identifierName();

  default DepInjectionFramework sourceFramework() {
    return NONE;
  }
}
