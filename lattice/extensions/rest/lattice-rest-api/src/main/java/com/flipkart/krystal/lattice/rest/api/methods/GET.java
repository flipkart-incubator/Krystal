package com.flipkart.krystal.lattice.rest.api.methods;

import static java.lang.annotation.ElementType.TYPE;

import com.flipkart.krystal.annos.ApplicableToElements;
import com.flipkart.krystal.core.KrystalElement.Vajram;
import java.lang.annotation.Target;

/** Lattice equivalent of {@link jakarta.ws.rs.GET} */
@Target(TYPE)
@ApplicableToElements(Vajram.class)
public @interface GET {}
