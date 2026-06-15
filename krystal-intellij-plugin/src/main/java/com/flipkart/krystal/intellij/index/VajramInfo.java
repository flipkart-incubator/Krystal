package com.flipkart.krystal.intellij.index;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;

import com.intellij.psi.PsiClass;

public record VajramInfo(PsiClass vajramClass, String vajramId, boolean trait) {

  public String facetsClassName() {
    return vajramId + FACETS_CLASS_SUFFIX;
  }

  public String requestClassName() {
    return vajramId + REQUEST_SUFFIX;
  }
}
