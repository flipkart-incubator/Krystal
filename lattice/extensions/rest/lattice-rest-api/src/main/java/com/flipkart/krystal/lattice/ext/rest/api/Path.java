package com.flipkart.krystal.lattice.ext.rest.api;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import java.lang.annotation.Target;

@ApplicableToElements(Vajram.class)
@Target(TYPE)
public @interface Path {
  String value();
}
