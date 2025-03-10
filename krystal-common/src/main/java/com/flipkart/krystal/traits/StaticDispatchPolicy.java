package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.facets.Dependency;
import jakarta.inject.Qualifier;
import java.lang.annotation.Annotation;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * When a vajram 'V1' adds a dependency on a trait, krystal uses a TraitBindingProvider to retrive
 * the conformant vajram which is bound to the trait. V1 can influence which concrete vajram is
 * bound by using a {@link jakarta.inject.Qualifier Qualifier} annotation on the dependency. This
 * allows krystal to support static dispatch via Guice-like depdendency injection for vajram
 * dependency invocations.
 */
public non-sealed interface StaticDispatchPolicy extends TraitDispatchPolicy {

  /**
   * Returns the concrete {@link VajramID} bound to the a trait for the given dependency.
   *
   * @param traitId The vajramId of the trait for which the concrete bound vajram is to be
   *     determined
   * @param dependency The depdency facet by which a vajram has added a dependency on the trait
   */
  VajramID get(Dependency dependency);

  /**
   * Returns the concrete {@link VajramID} bound to the a trait for the given qualifier.
   *
   * @param traitId The vajramId of the trait for which the concrete bound vajram is to be
   *     determined
   * @param dependency The depdency facet by which a vajram has added a dependency on the trait
   */
  VajramID get(@Nullable Annotation qualifier);

  static boolean isValidQualifier(@Nullable Annotation qualifier) {
    return qualifier == null || qualifier.annotationType().getAnnotation(Qualifier.class) != null;
  }
}
