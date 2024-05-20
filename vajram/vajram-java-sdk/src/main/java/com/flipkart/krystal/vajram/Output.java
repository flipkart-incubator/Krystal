package com.flipkart.krystal.vajram;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation declares the output facet of a vajram. <br>
 * <br>
 *
 * <p>When on a static method, this indicates that the method is responsible for computing the
 * output facet of the vajram.<br>
 * <br>
 *
 * <p>When on a field which also has the @{@link Dependency} annotation, this indicates that the
 * value of the dependency facet should be used as the output of the vajram.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Output {}
