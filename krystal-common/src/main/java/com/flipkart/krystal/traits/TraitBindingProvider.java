package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;

/**
 * When a vajram 'V1' adds a dependency on a trait, krystal uses a TraitBindingProvider to retrive
 * the conformant vajram which is bound to the trait. V1 can influence which concrete vajram is
 * bound by using a {@link jakarta.inject.Qualifier Qualifier} annotation on the dependency.
 */
public interface TraitBindingProvider {

  /**
   * Returns the concrete {@link VajramID} bound to the provided trait for the given dependency.
   *
   * @param traitId The vajramId of the trait for which the concrete bound vajram is to be
   *     determined
   * @param dependency The depdency facet by which a vajram has added a dependency on the trait
   */
  VajramID get(VajramID traitId, Dependency dependency);
}
