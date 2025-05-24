package com.flipkart.krystal.lattice.codegen;

import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DefaultSerdeProtocolProvider {
  @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context);
}
