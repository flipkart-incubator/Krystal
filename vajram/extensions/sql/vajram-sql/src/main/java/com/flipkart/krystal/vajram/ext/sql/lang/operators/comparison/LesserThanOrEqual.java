package com.flipkart.krystal.vajram.ext.sql.lang.operators.comparison;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Target;

/**
 * The SQL {@code <=} operator which checks if a value is less than or equal to another.
 *
 * <p>This operator is only valid on comparable types: numeric primitives and their boxed
 * equivalents ({@code int}, {@code long}, {@code short}, {@code float}, {@code double}), and
 * temporal types ({@code LocalDate}, {@code LocalDateTime}, {@code OffsetDateTime}).
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @LesserThanOrEqual
 * long maxAge();
 * }</pre>
 */
@Target(METHOD)
public @interface LesserThanOrEqual {}
