package com.flipkart.krystal.vajram.facets;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Declares a facet as a client provided input */
@Target(ElementType.FIELD)
public @interface Input {}
