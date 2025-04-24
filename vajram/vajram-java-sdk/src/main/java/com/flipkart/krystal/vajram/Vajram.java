package com.flipkart.krystal.vajram;

import com.flipkart.krystal.vajram.facets.Dependency;
import com.flipkart.krystal.vajram.facets.Output;
import com.flipkart.krystal.vajram.facets.resolution.Resolve;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation on a Vajram class which tells the vajram SDK that this vajram needs to be processed
 * by the Vajram annotation processors to generate model java classes and wrapper java classes by
 * looking for the relevant annotations on the facet fields and methods in the vajram. Examples of
 * these are inputs, {@link Dependency dependencies}, @{@link Resolve resolvers}, @{@link Output
 * output logic}, etc
 *
 * <p>In a vajram class hierarchy where classA extends classB etc., the simpleClassName of the class
 * which has this annotation will be used as the vajram id .
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Vajram {}
