package com.flipkart.krystal.lattice.ext.rest.api;

import static java.lang.annotation.ElementType.FIELD;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import java.lang.annotation.Target;

/** The body of a rest request/response */
@Target(FIELD)
@ApplicableToElements(Facet.class)
public @interface Body {}
