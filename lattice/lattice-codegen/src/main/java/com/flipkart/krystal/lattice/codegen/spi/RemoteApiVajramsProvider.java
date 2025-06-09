package com.flipkart.krystal.lattice.codegen.spi;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.common.collect.ImmutableList;
import javax.lang.model.element.TypeElement;

public interface RemoteApiVajramsProvider {
  ImmutableList<TypeElement> getRemotelyInvokedVajrams(LatticeCodegenContext latticeCodegenContext);
}
