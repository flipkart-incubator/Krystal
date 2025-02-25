package com.flipkart.krystal.vajram.traitbinding;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import com.flipkart.krystal.vajram.facets.specs.FacetSpec;

public interface TraitBindingProvider {
  VajramID get(VajramID vajramID, Dependency facetDef, VajramID depVajramId);
}
