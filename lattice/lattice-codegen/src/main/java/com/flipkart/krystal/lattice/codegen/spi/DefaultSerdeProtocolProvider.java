package com.flipkart.krystal.lattice.codegen.spi;

import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import javax.lang.model.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DefaultSerdeProtocolProvider {
  @Nullable TypeElement getDefaultSerializationProtocol(LatticeCodegenContext context);
}
