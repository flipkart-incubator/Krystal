package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.annos.ConformsToTrait;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A vajram trait defines a functional "behaviour" including the semantics of the inputs and the
 * output of a vajram, but does not speecify any business logic or implementation of the behaviour.
 * Multiple different @{@link Vajram}s may {@link ConformsToTrait conform to} a VajramTrait. This is
 * similar to a {@link FunctionalInterface} in java, traits in scala and rust etc. Application
 * runtimes can conditionally bind one or more vajrams to a Trait so that invocations of the trait
 * are delegated to and "fulfilled" by the bound vajram. This "Binding" is similar to dependency
 * injection where implementations are bound to interfaces in languages like Java.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface VajramTrait {}
