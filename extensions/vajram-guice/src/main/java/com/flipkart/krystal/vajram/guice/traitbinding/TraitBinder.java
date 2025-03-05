package com.flipkart.krystal.vajram.guice.traitbinding;

import com.flipkart.krystal.vajram.VajramTraitDef;
import com.google.inject.Binder;
import jakarta.inject.Qualifier;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

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

  public TraitBinder() {}

  void addBinding(TraitBinding traitBinding) {
    traitBindings.add(traitBinding);
  }

  public AnnotatedBindingBuilder bindTrait(Class<? extends VajramTraitDef<?>> traitDef) {
    return new AnnotatedBindingBuilder(this, traitDef);
  }
}
