package com.flipkart.krystal.lattice.codegen;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.lang.model.element.TypeElement;

public interface RemoteApiVajramsProvider {
  ImmutableList<TypeElement> getRemotelyInvokedVajrams(LatticeCodegenContext latticeCodegenContext);
}
