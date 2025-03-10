package com.flipkart.krystal.vajram.guice.traitbinding;

import static com.google.common.base.Preconditions.checkArgument;

import com.flipkart.krystal.data.Request;
import com.flipkart.krystal.vajram.TraitRequestRoot;
import com.google.inject.Binder;
import jakarta.inject.Qualifier;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A TraitBinder can be used to bind concrete vajrams to traits. The bindings can be unqualified,
 * meaning the binding applies to when a dependency does not have a qualifying annotation (an
 * annotation which itself has the {@link Qualifier} annotation on its type), or the bindings can be
 * qualified by an annotation type (used as a catch-all for all depdendencies which has that
 * annotation on the dependency facet), or the bindings can be qualified by an annotation instance
 * (used when a dependency has that exact annotation instance with given annotation parameters on
 * the dependency facet).
 *
 * <p>This class and its EDSL-based usage pattern is heavily inspired by {@link Binder} so that
 * developers used Guice can use this class with ease.
 *
 * @see Binder
 */
public final class TraitBinder {

  @Getter private final List<TraitBinding> traitBindings = new ArrayList<>();
  @Getter private final @Nullable Binder guiceBinder;

  public TraitBinder() {
    this.guiceBinder = null;
  }

  /**
   * If a guice binder is provided, then binding a trait request type to a vajram request type will
   * also bind them in the guice binder so that injecting an object of the trait request type is
   * injected, the corresponding vajram's request is injected.
   */
  public TraitBinder(Binder guiceBinder) {
    this.guiceBinder = guiceBinder;
  }

  void addBinding(TraitBinding traitBinding) {
    traitBindings.add(traitBinding);
  }

  public <T extends Request<?>> AnnotatedBindingBuilder<T> bindTrait(Class<T> traitReq) {
    checkArgument(
        traitReq.getAnnotation(TraitRequestRoot.class) != null,
        "Only Vajram Trait Request Interface can be bound");
    return new AnnotatedBindingBuilder(this, traitReq);
  }
}
