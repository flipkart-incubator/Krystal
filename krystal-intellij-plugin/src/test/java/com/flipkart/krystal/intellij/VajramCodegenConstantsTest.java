package com.flipkart.krystal.intellij;

import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACET_NAME_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.FACETS_CLASS_SUFFIX;
import static com.flipkart.krystal.vajram.codegen.common.models.Constants.REQUEST_SUFFIX;

import org.junit.Assert;
import org.junit.Test;

public class VajramCodegenConstantsTest {

  @Test
  public void facetNamingMatchesCodegen() {
    Assert.assertEquals("_n", FACET_NAME_SUFFIX);
    Assert.assertEquals("_Fac", FACETS_CLASS_SUFFIX);
    Assert.assertEquals("_Req", REQUEST_SUFFIX);
  }
}
