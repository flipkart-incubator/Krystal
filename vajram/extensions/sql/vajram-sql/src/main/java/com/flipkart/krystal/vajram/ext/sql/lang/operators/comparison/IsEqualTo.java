package com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * The SQL {@code =} operator which checks for equality
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @IsEqualTo
 * String nameEquals();
 * }</pre>
 */
@Target(METHOD)
public @interface IsEqualTo {}
