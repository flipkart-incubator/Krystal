package com.flipkart.krystal.lattice.rest.api;

import static java.lang.annotation.ElementType.FIELD;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Facet;
import java.lang.annotation.Target;

@Target(FIELD)
@ApplicableToElements(Facet.class)
public @interface QueryParam {}
