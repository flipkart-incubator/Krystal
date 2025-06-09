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
   * @param needsRequestScopedHeaders
   * @param remotelyInvocableVajrams
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
