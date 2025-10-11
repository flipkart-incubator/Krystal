package com.flipkart.krystal.traits;

import com.flipkart.krystal.core.VajramID;
import com.flipkart.krystal.data.Request;
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
public abstract non-sealed class StaticDispatchPolicy implements TraitDispatchPolicy {

  /**
   * Returns the concrete {@link VajramID} bound to the a trait for the given dependency.
   *
   * @param dependency The depdency facet by which a vajram has added a dependency on the trait
   */
  public abstract VajramID getDispatchTarget(Dependency dependency);

  /**
   * Returns the concrete {@link VajramID} bound to a trait for the given qualifier.
   *
   * @param qualifier The qualifier by which a dispatch target is to be selected
   */
  public abstract VajramID getDispatchTarget(@Nullable Annotation qualifier);

  public static boolean isValidQualifier(@Nullable Annotation qualifier) {
    return qualifier == null || qualifier.annotationType().getAnnotation(Qualifier.class) != null;
  }

  @Override
  public final @Nullable VajramID getDispatchTarget(
      @Nullable Dependency dependency, Request<?> request) {
    if (dependency == null) {
      return null;
    }
    return getDispatchTarget(dependency);
  }
}
