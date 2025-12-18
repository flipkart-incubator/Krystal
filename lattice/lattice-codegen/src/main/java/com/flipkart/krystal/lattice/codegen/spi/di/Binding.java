package com.flipkart.krystal.lattice.codegen.spi.di;

public sealed interface Binding permits ImplTypeBinding, NullBinding, ProviderMethod {
  String identifierName();
}
