package com.flipkart.krystal.model;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Target;

/**
 * Place it on the first value of an enum to mark it as the default value. Krystal modelling
 * framework mandates this.
 */
@Target(FIELD)
public @interface DefaultValue {}
