package com.flipkart.krystal.lattice.codegen;

import static com.flipkart.krystal.datatypes.Trilean.UNKNOWN;

import com.flipkart.krystal.datatypes.Trilean;
import com.google.common.collect.ImmutableList;
import javax.lang.model.element.TypeElement;
import lombok.Builder;

public interface LatticeAppCodeGenAttrsProvider {
  LatticeAppCodeGenAttributes get(LatticeCodegenContext context);

  /**
   * @param needsRequestScopedHeaders
   * @param remotelyInvokedVajrams
   */
  @Builder
  record LatticeAppCodeGenAttributes(
      Trilean needsRequestScopedHeaders, ImmutableList<TypeElement> remotelyInvokedVajrams) {

    public LatticeAppCodeGenAttributes {
      if (needsRequestScopedHeaders == null) {
        needsRequestScopedHeaders = UNKNOWN;
      }
      if (remotelyInvokedVajrams == null) {
        remotelyInvokedVajrams = ImmutableList.of();
      }
    }
  }
}
