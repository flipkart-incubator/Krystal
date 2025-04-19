package com.flipkart.krystal.vajram;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Trait defines a functionality or a behaviour. This includes the semantics of the inputs and the
 * output of a vajram, but does not specify any business logic or implementation of the behaviour.
 * Multiple different {@link Vajram @Vajram}s may conform to a Trait my implementing the interface
 * which defines a trait. This is similar to a {@link FunctionalInterface} in java, traits in scala
 * and rust etc. Application runtimes can conditionally bind one or more vajrams to a Trait so that
 * invocations of the trait are delegated to and "fulfilled" by the bound vajram. This "Binding" is
 * similar to dependency injection where implementations are bound to interfaces in languages like
 * Java.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Trait {}
