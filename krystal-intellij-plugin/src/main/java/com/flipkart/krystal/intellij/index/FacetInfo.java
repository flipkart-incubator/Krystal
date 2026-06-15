package com.flipkart.krystal.intellij.index;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_SPEC_SUFFIX;

import com.intellij.psi.PsiType;

public record FacetInfo(
    String name,
    FacetKind kind,
    PsiType type,
    boolean mandatoryOnServer,
    boolean optionalForClient,
    String dependencyVajramName,
    String qualifiedName) {

  public String facetNameConstant() {
    return name + FACET_NAME_SUFFIX;
  }

  public String facetSpecConstant() {
    return name + FACET_SPEC_SUFFIX;
  }
}
