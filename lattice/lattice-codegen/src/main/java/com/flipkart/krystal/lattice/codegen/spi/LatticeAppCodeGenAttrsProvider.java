package com.flipkart.krystal.lattice.codegen.spi;

import static com.flipkart.krystal.datatypes.Trilean.UNKNOWN;

import com.flipkart.krystal.datatypes.Trilean;
import com.flipkart.krystal.lattice.codegen.LatticeCodegenContext;
import com.google.common.collect.ImmutableList;
import javax.lang.model.element.TypeElement;
import lombok.Builder;

public interface LatticeAppCodeGenAttrsProvider {
  LatticeAppCodeGenAttributes get(LatticeCodegenContext context);

  /**
   * Attributes which are using the Lattice Application code generation
   *
   * @param needsRequestScopedHeaders Whether the lattice app needs support for request scoping in
   *     its Dependency Injection
   * @param remotelyInvocableVajrams The set of remotely invocable vajrams which are exposed as APIs
   *     in a server
   */
  @Builder
  record LatticeAppCodeGenAttributes(
      Trilean needsRequestScopedHeaders, ImmutableList<TypeElement> remotelyInvocableVajrams) {

    public LatticeAppCodeGenAttributes {
      if (needsRequestScopedHeaders == null) {
        needsRequestScopedHeaders = UNKNOWN;
      }
      if (remotelyInvocableVajrams == null) {
        remotelyInvocableVajrams = ImmutableList.of();
      }
    }
  }
}
